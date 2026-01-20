package de.nogaemer.springhomepage.main.meals

import de.nogaemer.springhomepage.appwrite.AppwriteService
import de.nogaemer.springhomepage.exceptions.AlreadyReported
import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.exceptions.UnitNotFoundException
import de.nogaemer.springhomepage.main.images.Image
import de.nogaemer.springhomepage.main.ingredients.Ingredient
import de.nogaemer.springhomepage.main.ingredients.IngredientService
import de.nogaemer.springhomepage.main.meals.dto.MealCardDto
import de.nogaemer.springhomepage.main.meals.dto.MealDto
import de.nogaemer.springhomepage.main.meals.dto.MealIngredientDto
import de.nogaemer.springhomepage.main.meals.import.Chefkoch
import de.nogaemer.springhomepage.main.meals.models.Meal
import de.nogaemer.springhomepage.main.meals.models.MealImportMethod
import de.nogaemer.springhomepage.main.meals.models.MealIngredient
import de.nogaemer.springhomepage.main.notes.Note
import de.nogaemer.springhomepage.main.ratings.Rating
import de.nogaemer.springhomepage.main.ratings.RatingService
import de.nogaemer.springhomepage.main.tags.Tag
import de.nogaemer.springhomepage.main.tags.TagService
import de.nogaemer.springhomepage.main.units.IngredientUnit
import de.nogaemer.springhomepage.main.units.UnitService
import de.nogaemer.springhomepage.main.utils.AggregationUtils
import org.bson.BsonNull
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

/**
 * Service layer for meal management operations including CRUD, search, filtering, and import.
 *
 * This service handles all business logic for meal entities, including:
 * - CRUD operations with cache management
 * - Complex MongoDB aggregation queries for optimized data retrieval
 * - Ingredient resolution and validation
 * - External recipe imports with duplicate detection
 * - Cleanup of associated resources (Appwrite images, ratings)
 *
 * ## Caching Strategy
 * Uses Spring Cache with two cache regions:
 * - **"meals"**: Individual meal caches by ID
 * - **"allMeals"**: Cached list of all meal cards
 *
 * Cache invalidation occurs on create, update, and delete operations to maintain consistency.
 *
 * ## MongoDB Aggregation
 * Many methods use aggregation pipelines instead of simple queries for:
 * - Efficient resolution of @DocumentReference relationships
 * - Complex filtering and sorting logic
 * - Denormalization of nested data for DTOs
 * - Performance optimization by projecting only required fields
 *
 * ## Transaction Boundaries
 * Currently, operations are not wrapped in transactions. Consider adding @Transactional
 * for operations that modify multiple collections (e.g., deleteById which affects
 * meals, ratings, and Appwrite storage).
 *
 * ## Performance Considerations
 * - [findAll] is cached to reduce database load for frequently accessed meal lists
 * - [findById] uses aggregation with $lookup to resolve all relationships in one query
 * - [filterMeals] includes complex filtering and user-specific rating calculations
 *
 * @property repository MongoDB repository for meal persistence
 * @property ratingService Service for managing meal ratings
 * @property tagService Service for tag management
 * @property unitService Service for ingredient unit resolution
 * @property ingredientService Service for ingredient management
 * @property applicationContext Spring context for proxy self-reference (cache aspect)
 * @property mongoTemplate MongoDB template for aggregation queries
 * @property appwriteService Service for Appwrite storage operations (image deletion)
 *
 * @see Meal
 * @see MealDto
 * @see MealCardDto
 * @see MealRepository
 */
@Service
class MealService(
    val repository: MealRepository,
    val ratingService: RatingService,
    val tagService: TagService,
    val unitService: UnitService,
    val ingredientService: IngredientService,
    val applicationContext: ApplicationContext,
    private val mongoTemplate: MongoTemplate,
    private val appwriteService: AppwriteService
) {

    /**
     * Returns a proxy reference to this service to ensure cache annotations work correctly.
     *
     * Required because Spring AOP proxies don't intercept internal method calls.
     * Methods that need cache aspect behavior must be called via self().method().
     */
    private fun self(): MealService = applicationContext.getBean(MealService::class.java)

    /**
     * Converts MealIngredientDto list to MealIngredient entities with resolved unit references.
     *
     * ## Resolution Process
     * For each ingredient DTO:
     * 1. Extracts unit ID from IngredientUnitDto
     * 2. Validates unit ID format (ObjectId)
     * 3. Fetches IngredientUnit entity from database
     * 4. Creates MealIngredient with resolved references
     *
     * ## Error Handling
     * - Invalid unit ID format → IdNotFoundException
     * - Unit not found in database → UnitNotFoundException
     *
     * @param dtoIngredients List of ingredient DTOs from client request
     * @return List of resolved MealIngredient entities ready for persistence
     * @throws IdNotFoundException If unit ID is not a valid ObjectId
     * @throws UnitNotFoundException If unit does not exist in database
     */
    private fun resolveIngredients(dtoIngredients: List<MealIngredientDto>): List<MealIngredient> {
        return dtoIngredients.map { dto ->
            val unitObj = dto.unit.let {
                try {
                    unitService.findById(it.id)
                } catch (_: IllegalArgumentException) {
                    throw IdNotFoundException("Invalid unit id '${it}'")
                }
            }

            unitObj ?: throw UnitNotFoundException("Could not find unit for ingredient '${dto.ingredient.name}'")

            MealIngredient(
                amount = dto.amount,
                ingredient = dto.ingredient,
                unit = unitObj
            )
        }
    }

    /**
     * Retrieves all meals as lightweight card DTOs with caching.
     *
     * ## Caching
     * Results are cached in the "allMeals" cache region. Cache is invalidated on any
     * create, update, or delete operation.
     *
     * ## MongoDB Aggregation
     * Uses aggregation pipeline stages from AggregationUtils.getMealCardProjectionStages():
     * - Projects only fields needed for card display (id, name, rating, time, difficulty, images)
     * - Denormalizes nested structures for efficient serialization
     * - Avoids loading full meal details and @DocumentReference fields
     *
     * ## Performance
     * Optimized for list displays. Returns minimal data per meal compared to findById().
     *
     * @return List of all meals as MealCardDto, cached after first call
     */
    @Cacheable("allMeals")
    fun findAll(): List<MealCardDto> {
        val aggregation = newAggregation(
            *AggregationUtils.getMealCardProjectionStages().toTypedArray()
        )

        return mongoTemplate.aggregate(aggregation, "meals", MealCardDto::class.java).mappedResults
    }

    /**
     * Retrieves a single meal by ID with all relationships populated.
     *
     * ## MongoDB Aggregation Pipeline
     * Uses complex aggregation to resolve @DocumentReference fields efficiently:
     * 1. **$match**: Filters to single meal by _id
     * 2. **$lookup**: Populates tags, ratings, notes from referenced collections
     * 3. **$lookup**: Resolves ingredient and unit references within embedded ingredients array
     * 4. **$project**: Maps fields and denormalizes ingredient references using $map
     *
     * ## Why Aggregation vs Simple Query
     * Standard repository.findById() would trigger N+1 queries due to @DocumentReference
     * lazy loading. Aggregation resolves all relationships in a single database roundtrip.
     *
     * ## Projection Mapping
     * Results are mapped to MealProjection first, then converted to Meal entity.
     * This bypasses Spring Data's @DocumentReference resolution logic which would
     * cause additional queries.
     *
     * ## Performance Considerations
     * - Single aggregation query replaces multiple individual lookups
     * - Consider indexing on tags, ratings, notes for efficient $lookup operations
     * - Ingredient and unit lookups benefit from indexes on _id fields
     *
     * @param id MongoDB ObjectId of the meal
     * @return Fully populated Meal entity with all relationships resolved
     * @throws IllegalArgumentException If meal with given ID not found
     */
    fun findById(id: ObjectId): Meal {
        val matchStage = match(Criteria.where("_id").`is`(id))

        val lookupTags = lookup("tags", "tags", "_id", "tags")
        val lookupRatings = lookup("ratings", "ratings", "_id", "ratings")
        val lookupNotes = lookup("notes", "notes", "_id", "notes")

        val lookupIngredients = lookup("ingredients", "ingredients.ingredient", "_id", "resolvedIngredients")
        val lookupUnits = lookup("units", "ingredients.unit", "_id", "resolvedUnits")

        val projectStage = project()
            .and("_id").`as`("id")
            .and("name").`as`("name")
            .and("instructions").`as`("instructions")
            .and("images").`as`("images")
            .and("difficulty").`as`("difficulty")
            .and("time").`as`("time")
            .and("portions").`as`("portions")
            .and("calories").`as`("calories")
            .and("url").`as`("url")
            .and("tags").`as`("tags")
            .and("ratings").`as`("ratings")
            .and("notes").`as`("notes")
            .and("rating").`as`("rating")
            .and {
                Document(
                    "\$map", Document()
                        .append("input", "\$ingredients")
                        .append("as", "item")
                        .append(
                            "in", Document()
                                .append("name", "\$\$item.name")
                                .append("amount", "\$\$item.amount")
                                .append(
                                    "ingredient",
                                    AggregationUtils.lookupObject("resolvedIngredients", "ingredient")
                                )
                                .append(
                                    "unit",
                                    AggregationUtils.lookupObject("resolvedUnits", "unit")
                                )
                        )
                )
            }.`as`("ingredients")

        val aggregation = newAggregation(
            matchStage,
            lookupTags,
            lookupRatings,
            lookupNotes,
            lookupIngredients,
            lookupUnits,
            projectStage
        )

        // Use the Projection class instead of Meal::class.java to bypass @DocumentReference logic
        val results = mongoTemplate.aggregate(aggregation, "meals", MealProjection::class.java)
        println("unique Aggregation: $aggregation.to")
        val projection = results.uniqueMappedResult ?: throw IllegalArgumentException("Meal with id $id not found")

        // Map Projection back to Meal
        return Meal(
            name = projection.name,
            ingredients = projection.ingredients.map {
                MealIngredient(
                    it.name,
                    it.amount,
                    Ingredient(
                        it.ingredient?.name ?: throw IllegalArgumentException("Ingredient with id ${it.ingredient?.id} not found"),
                        it.ingredient.category,
                    ).apply { this.id = it.ingredient.id},
                    it.unit)
            },
            instructions = projection.instructions,
            images = projection.images,
            difficulty = projection.difficulty,
            time = projection.time,
            portions = projection.portions,
            calories = projection.calories,
            url = projection.url,
            tags = projection.tags.toMutableList(),
            rating = projection.rating
        ).apply {
            this.id = projection.id
            this.ratings = projection.ratings
            this.notes = projection.notes
        }
    }

    /**
     * Searches for meals by name using case-insensitive regex matching.
     *
     * ## Search Behavior
     * - Null or empty name returns all meals via cached findAll()
     * - Non-empty name performs case-insensitive partial matching
     * - Results returned as lightweight MealCardDto for performance
     *
     * ## MongoDB Aggregation Pipeline
     * 1. **$match**: Case-insensitive regex filter on name field
     * 2. **Projection stages**: Same as findAll() to create MealCardDto
     *
     * ## Performance
     * - Benefits from index on name field for efficient regex search
     * - Fallback to cached findAll() avoids unnecessary queries for empty search
     *
     * @param name Search string for partial name matching (null/empty returns all meals)
     * @return List of matching meals as MealCardDto
     */
    fun searchByName(name: String?): List<MealCardDto> {
        if (name.isNullOrEmpty()) return self().findAll()

        val desiredName = name

        val matchName = match(Criteria.where("name").regex(desiredName, "i"))

        val stages = mutableListOf<AggregationOperation>()
        stages.add(matchName)
        stages.addAll(AggregationUtils.getMealCardProjectionStages())

        val aggregation = newAggregation(
            *stages.toTypedArray()
        )

        return mongoTemplate.aggregate(aggregation, "meals", MealCardDto::class.java).mappedResults
    }

    /**
     * Creates a new meal from a MealDto with duplicate detection and cache invalidation.
     *
     * ## Process
     * 1. Checks for duplicate meal name (case-insensitive)
     * 2. Resolves ingredient DTOs to MealIngredient entities with full references
     * 3. Creates new Meal entity from DTO
     * 4. Saves to database via repository
     * 5. Invalidates allMeals cache
     *
     * ## Cache Management
     * [@Caching] annotation evicts "allMeals" cache after successful creation.
     *
     * ## Validation
     * - Meal name must be unique (throws AlreadyReported if duplicate)
     * - Ingredient units must exist (validated in resolveIngredients)
     *
     * @param meal MealDto containing all meal data
     * @return Saved Meal entity with generated ID
     * @throws AlreadyReported If meal with same name already exists
     * @throws IdNotFoundException If any ingredient unit ID is invalid
     * @throws UnitNotFoundException If any ingredient unit doesn't exist
     */
    @Caching(
        evict = [
            CacheEvict(cacheNames = ["allMeals"], allEntries = true)
        ]
    )
    fun create(meal: MealDto): Meal {
        repository.findByName(meal.name)?.let {
            throw AlreadyReported("Meal with name ${meal.name} already exists", meal)
        }

        val ingredients = resolveIngredients(meal.ingredients)

        val newMeal = Meal(
            name = meal.name,
            ingredients = ingredients,
            instructions = meal.instructions,
            images = meal.images,
            difficulty = meal.difficulty,
            time = meal.time,
            portions = meal.portions,
            calories = meal.calories,
            tags = meal.tags
        )

        return repository.save(newMeal)
    }

    /**
     * Creates a new meal from a Meal entity with duplicate detection and cache invalidation.
     *
     * Overloaded version that accepts a fully constructed Meal entity rather than DTO.
     * Used internally by import operations and programmatic meal creation.
     *
     * ## Process
     * 1. Checks for duplicate meal name
     * 2. Saves entity directly (assumes ingredients already resolved)
     * 3. Invalidates allMeals cache
     *
     * @param meal Fully constructed Meal entity
     * @return Saved Meal entity with generated ID
     * @throws AlreadyReported If meal with same name already exists
     */
    @Caching(
        evict = [
            CacheEvict(cacheNames = ["allMeals"], allEntries = true)
        ]
    )
    fun create(meal: Meal): Meal {
        repository.findByName(meal.name)?.let {
            throw AlreadyReported("Meal with name ${meal.name} already exists", meal)
        }

        return repository.save(meal)
    }

    /**
     * Asynchronously imports a meal from an external recipe website.
     *
     * ## Async Execution
     * Runs on separate thread via @Async annotation. Returns CompletableFuture for
     * non-blocking execution. Useful for time-consuming web scraping operations.
     *
     * ## Import Process
     * 1. Validates URL matches expected domain for import method
     * 2. Delegates to appropriate parser (e.g., Chefkoch)
     * 3. Parser scrapes recipe data and creates Meal entity
     * 4. Optionally saves to database if save=true
     * 5. Checks for duplicate URL before saving
     *
     * ## Duplicate Detection
     * Uses repository.countByUrl() to prevent importing same recipe multiple times.
     * Throws AlreadyReported if URL already exists.
     *
     * ## Currently Supported Sources
     * - **CHEFKOCH**: chefkoch.de recipes
     *
     * @param tag Import method enum indicating source website
     * @param url Complete URL of the recipe page
     * @param save Whether to save imported meal to database (false for preview)
     * @return CompletableFuture containing imported Meal entity
     * @throws IllegalArgumentException If URL doesn't match expected domain
     * @throws AlreadyReported If recipe URL already imported (when save=true)
     */
    @Async
    fun importMealAsync(tag: MealImportMethod, url: String, save: Boolean = true): CompletableFuture<Meal> {
        return CompletableFuture.supplyAsync {
            when (tag) {
                MealImportMethod.CHEFKOCH -> {
                    if (!url.contains("chefkoch.de")) throw IllegalArgumentException("Url is not from Chefkoch")
                    val meal = Chefkoch(tagService, unitService, ingredientService).getMealFromUrl(url)

                    if (!save) return@supplyAsync meal

                    if (repository.countByUrl(url) >= 1)
                        throw AlreadyReported("Meal with url $url already exists", meal)

                    repository.save(meal)
                }
            }
        }
    }

    /**
     * Fetches lightweight meal basic information using aggregation projection.
     *
     * Returns only meal name and first image URL without loading DocumentReference
     * fields (tags, ratings, notes, ingredients, units). This avoids N+1 query issues
     * when only basic meal info is needed for denormalization.
     *
     * ## Performance
     * Uses MongoDB aggregation with projection to fetch only required fields.
     * Does not trigger lazy loading of @DocumentReference relationships.
     *
     * ## Use Cases
     * - Denormalizing meal data in cook history
     * - Denormalizing meal data in daily meal plans
     * - Any scenario requiring just name and image
     *
     * ## Aggregation Pipeline
     * 1. Match by meal ID
     * 2. Project only name and first image thumbnail
     *
     * @param id MongoDB ObjectId of the meal
     * @return MealBasicInfoDto with name and image URL
     * @throws IdNotFoundException If meal with given ID not found
     */
    fun getMealBasicInfo(id: ObjectId): de.nogaemer.springhomepage.main.meals.dto.MealBasicInfoDto {
        val matchStage = match(Criteria.where("_id").`is`(id))
        
        val projectStage = project()
            .and("name").`as`("name")
            .andExpression("{ \$arrayElemAt: [ '\$images.thumbnail', 0 ] }").`as`("imageUrl")
        
        val aggregation = newAggregation(matchStage, projectStage)
        
        val result: AggregationResults<de.nogaemer.springhomepage.main.meals.dto.MealBasicInfoDto> = 
            mongoTemplate.aggregate(
                aggregation,
                "meals",
                de.nogaemer.springhomepage.main.meals.dto.MealBasicInfoDto::class.java
            )
        
        return result.uniqueMappedResult 
            ?: throw IdNotFoundException("Meal not found with id: $id")
    }

    /**
     * Deletes a meal and cleans up all associated resources.
     *
     * ## Deletion Flow
     * 1. Fetches full meal entity by ID
     * 2. Deletes associated images from Appwrite storage
     * 3. Deletes all user ratings for this meal
     * 4. Deletes meal document from MongoDB
     * 5. Invalidates relevant caches
     *
     * ## Appwrite Image Cleanup
     * Iterates through meal.images and deletes each file from Appwrite storage using
     * the deleteUrls field. Failures are logged but don't stop the deletion process,
     * preventing orphaned meal documents if image deletion fails.
     *
     * ## Cache Management
     * [@Caching] annotation evicts:
     * - Specific meal from "meals" cache by ID
     * - All entries from "allMeals" cache
     *
     * ## Related Data Cleanup
     * - Ratings are deleted via ratingService.deleteRatingsByMeal()
     * - Notes are handled via @DocumentReference cascade (if configured)
     * - Tags remain in system (many-to-many relationship)
     *
     * ## Error Handling
     * - Image deletion failures are caught and logged (printed to console)
     * - Meal deletion proceeds even if some image files can't be deleted
     * - Consider adding proper logging framework instead of println
     *
     * ## Transaction Considerations
     * This operation modifies multiple resources (MongoDB, Appwrite storage).
     * Not wrapped in a transaction, so partial failures are possible. Consider
     * implementing compensation logic or distributed transaction pattern.
     *
     * @param id MongoDB ObjectId of the meal to delete
     * @throws IllegalArgumentException If meal with given ID not found
     */
    @Caching(
        evict = [
            CacheEvict(cacheNames = ["meals"], key = "#id"),
            CacheEvict(cacheNames = ["allMeals"], allEntries = true)
        ]
    )
    fun deleteById(id: ObjectId) {
        val meal = self().findById(id)
        // Delete files from Appwrite if present
        meal.images?.forEach { image ->
            image.deleteUrls?.forEach { fileId ->
                try {
                    if (fileId.isNotBlank()) {
                        appwriteService.deleteImage(fileId)
                    }
                } catch (e: Exception) {
                    // log and continue - don't fail whole delete if file deletion fails
                    // using println to avoid introducing logger; adjust if you have a logger
                    println("Failed to delete image file $fileId: ${e.message}")
                }
            }
        }

        ratingService.deleteRatingsByMeal(meal)
        repository.deleteById(id)
    }

    /**
     * Updates an existing meal with new data from MealDto.
     *
     * ## Update Process
     * 1. Fetches existing meal by ID
     * 2. Resolves ingredient DTOs to MealIngredient entities
     * 3. Creates updated copy with new data
     * 4. Preserves system-managed fields (id, url, rating, ratings, notes)
     * 5. Saves updated meal
     * 6. Invalidates relevant caches
     *
     * ## Cache Management
     * [@Caching] annotation evicts:
     * - Specific meal from "meals" cache by ID
     * - All entries from "allMeals" cache
     *
     * ## Field Handling
     * - **Updated**: name, ingredients, instructions, difficulty, time, images, portions, calories, tags
     * - **Preserved**: id, url, rating, ratings (list), notes (list)
     * - URL is preserved to maintain import source tracking
     * - Rating and relationships are recalculated/managed separately
     *
     * ## Data Copy Pattern
     * Uses Kotlin data class copy() method for immutable update pattern, then applies
     * preserved fields separately. This ensures clean separation between DTO fields
     * and system-managed state.
     *
     * @param id MongoDB ObjectId of meal to update
     * @param meal MealDto containing updated data
     * @return Updated Meal entity
     * @throws IdNotFoundException If meal with given ID not found
     * @throws IdNotFoundException If any ingredient unit ID is invalid
     * @throws UnitNotFoundException If any ingredient unit doesn't exist
     */
    @Caching(
        evict = [
            CacheEvict(cacheNames = ["meals"], key = "#id"),
            CacheEvict(cacheNames = ["allMeals"], allEntries = true)
        ]
    )
    fun update(id: ObjectId, meal: MealDto): Meal {
        val originalMeal = repository.findById(id).orElseThrow {
            IdNotFoundException("Meal with id $id not found")
        }

        val resolvedIngredients = resolveIngredients(meal.ingredients)

        val updatedMeal = originalMeal.copy(
            name = meal.name,
            ingredients = resolvedIngredients,
            instructions = meal.instructions,
            difficulty = meal.difficulty,
            time = meal.time,
            images = meal.images,
            portions = meal.portions,
            calories = meal.calories,
            tags = meal.tags,
            url = originalMeal.url,
            rating = originalMeal.rating
        ).apply {
            this.id = originalMeal.id
            this.ratings = originalMeal.ratings
            this.notes = originalMeal.notes
        }

        return repository.save(updatedMeal)
    }

    /**
     * Filters meals by name, tags, time, and user ratings with advanced scoring.
     *
     * This is the most complex query in the service, using extensive MongoDB aggregation
     * to support multi-dimensional filtering and user-specific rating calculations.
     *
     * ## Filter Parameters
     * - **name**: Case-insensitive partial name matching (empty/null matches all)
     * - **tags**: Comma-separated tag list (meals must have at least one matching tag)
     * - **time**: Maximum cooking time in minutes (filters 0 to time)
     * - **users**: Comma-separated user IDs for filtering by user-specific ratings
     *
     * ## MongoDB Aggregation Pipeline
     * 1. **$match**: Filters by name regex, time range, and tags
     * 2. **$lookup**: Joins with ratings collection to get all user ratings
     * 3. **$addFields (filterUserRatingsByUserId)**: Uses $filter to keep only ratings from specified users
     * 4. **$addFields (averageUserRating)**: Calculates average rating from filtered user ratings using $map and $sum
     * 5. **$sort**: Orders by averageUserRating (if users specified) or overall rating
     * 6. **Projection stages**: Converts to MealCardDto with optional userRatings field
     *
     * ## Rating Calculation Logic
     * - If no users specified: Sort by meal's overall rating field
     * - If users specified: Calculate and sort by average rating from those users only
     * - Average calculation handles empty ratings (returns 0) and null ratings (excluded)
     *
     * ## Complex $filter and $map Operations
     * Uses nested MongoDB operators:
     * - **$filter**: Filters ratings array to include only specified users
     * - **$map**: Transforms ratings array for averaging
     * - **$cond**: Conditional logic for null handling and empty array checks
     * - **$divide/$sum**: Calculates average from filtered ratings
     *
     * ## Performance Considerations
     * - Recommend indexes on: name (text/regex), tags (multi-key), time, ratings.userId
     * - User filtering requires ratings lookup and array processing (expensive for large datasets)
     * - Consider caching results for common filter combinations
     *
     * ## Use Cases
     * - General meal browsing with basic filters
     * - User-specific recommendations based on friend/family ratings
     * - Tag-based categorization (e.g., "vegetarian", "quick meals")
     * - Time-constrained meal planning (e.g., "under 30 minutes")
     *
     * @param name Optional name search term (null/empty matches all)
     * @param _users Optional comma-separated user IDs for rating filtering
     * @param _tags Optional comma-separated tags for category filtering
     * @param time Optional maximum cooking time in minutes (null defaults to 1000)
     * @return List of filtered meals as MealCardDto, sorted by relevant rating
     * @throws IllegalArgumentException If user IDs are not valid ObjectIds
     */
    fun filterMeals(
        name: String?,
        _users: String?,
        _tags: String?,
        time: Int?
    ): List<MealCardDto> {
        val desiredTags = _tags?.split(",") ?: emptyList()
        val desiredName = name ?: ""
        val minTime = 0
        val maxTime = time ?: 1000
        var userIds: List<ObjectId>

        try {
            userIds = _users?.split(",")?.map { user -> ObjectId(user) } ?: emptyList()
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid userIds")
        }

        val tagsCriteria = if (desiredTags.isNotEmpty()) {
            Criteria.where("tags").`in`(desiredTags)
        } else {
            Criteria.where("tags").exists(true)
        }


        // Stage 1: Match name and time
        val matchNameAndTime = match(
            Criteria()
                .andOperator(
                    Criteria.where("name").regex(desiredName, "i"),
                    Criteria.where("time").gte(minTime).lte(maxTime),
                    tagsCriteria
                )
        )

        // Stage 2: Lookup ratings
        val lookupRatings = lookup("ratings", "_id", "mealId", "userRatings")

        // Stage 5: Add fields to filter userRatings by userIds
        val filterRatings = Document(
            "\$filter", Document()
                .append("input", "\$userRatings")
                .append("as", "rating")
                .append(
                    "cond", Document(
                        "\$in", listOf(
                            "\$\$rating.userId", userIds
                        )
                    )
                )
        )

        val filterUserRatingsByUserId = addFields().addFieldWithValue(
            "userRatings",
            filterRatings
        ).build()

        // Stage 6: Unwind userRatings
        val addFieldsOperation = addFields().addFieldWithValue(
            "averageUserRating",
            Document(
                "\$cond", Document()
                    .append("if", Document("\$eq", listOf(Document("\$size", "\$userRatings"), 0)))
                    .append("then", 0)
                    .append(
                        "else", Document(
                            "\$divide", listOf(
                                Document(
                                    "\$sum",
                                    Document(
                                        "\$map",
                                        Document("input", "\$userRatings")
                                            .append("as", "userRating")
                                            .append(
                                                "in",
                                                Document(
                                                    "\$cond",
                                                    Document(
                                                        "if",
                                                        Document("\$ne", listOf("\$\$userRating.rating", BsonNull()))
                                                    ).append("then", "\$\$userRating.rating")
                                                        .append("else", 0L)
                                                )
                                            )
                                    )
                                ),
                                Document("\$size", "\$userRatings")
                            )
                        )
                    )
            )
        ).build()

        val sortByAverageRating = if (userIds.isEmpty()) {
            sort(Sort.by(Sort.Order.desc("rating")))
        } else {
            sort(Sort.by(Sort.Order.desc("averageUserRating")))
        }

        // Define the aggregation pipeline
        val stages = mutableListOf<AggregationOperation>()
        stages.add(matchNameAndTime)
        stages.add(lookupRatings)
        stages.add(filterUserRatingsByUserId)
        stages.add(addFieldsOperation)
        stages.add(sortByAverageRating)
        stages.addAll(AggregationUtils.getMealCardProjectionStages())

        val aggregation = newAggregation(
            *stages.toTypedArray()
        )

        // Execute the aggregation
        val results: AggregationResults<MealCardDto> =
            mongoTemplate.aggregate(aggregation, "meals", MealCardDto::class.java)

        return results.mappedResults
    }
}

/**
 * Internal projection class for mapping MongoDB aggregation results to Meal entities.
 *
 * This class is used in [MealService.findById] to receive aggregation pipeline results
 * before converting to the final Meal entity. It bypasses Spring Data's @DocumentReference
 * lazy loading mechanism which would trigger additional queries.
 *
 * ## Purpose
 * - Receives denormalized aggregation output
 * - Contains fully resolved relationships as plain objects (not DBRef)
 * - Intermediate format before Meal entity reconstruction
 *
 * @property id MongoDB ObjectId
 * @property name Meal name
 * @property instructions Cooking steps
 * @property images Image metadata
 * @property difficulty Difficulty level
 * @property time Cooking time
 * @property portions Serving count
 * @property calories Calories per serving
 * @property url Source URL
 * @property tags Resolved Tag entities
 * @property ratings Resolved Rating entities
 * @property notes Resolved Note entities
 * @property rating Calculated average rating
 * @property ingredients List of projected ingredient data
 */
private data class MealProjection(
    val id: ObjectId,
    val name: String,
    val instructions: List<String>,
    val images: List<Image>?,
    val difficulty: String,
    val time: Long,
    val portions: Int,
    val calories: Int,
    val url: String,
    val tags: List<Tag>,
    val ratings: List<Rating>,
    val notes: List<Note>,
    val rating: Double,
    val ingredients: List<MealIngredientProjection>
)

/**
 * Internal projection class for ingredient data within MealProjection.
 *
 * Represents a denormalized ingredient from MongoDB aggregation with resolved
 * references to ingredient and unit collections.
 *
 * @property name Display name for ingredient
 * @property amount Quantity string
 * @property ingredient Resolved ingredient data
 * @property unit Resolved unit entity
 */
private data class MealIngredientProjection(
    val name: String,
    val amount: String,
    val ingredient: IngredientProjection?,
    val unit: IngredientUnit?
)

/**
 * Internal projection class for ingredient entity data.
 *
 * Simplified ingredient representation from aggregation without full entity complexity.
 *
 * @property id Ingredient ObjectId
 * @property name Ingredient name
 * @property category Ingredient category
 * @property unit Default unit ObjectId reference
 */
private data class IngredientProjection(
    var id: ObjectId,
    val name: String,
    val category: String,
    val unit: ObjectId
)


