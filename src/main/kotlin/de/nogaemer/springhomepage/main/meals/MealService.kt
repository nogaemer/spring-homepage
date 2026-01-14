package de.nogaemer.springhomepage.main.meals

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

@Service
class MealService(
    val repository: MealRepository,
    val ratingService: RatingService,
    val tagService: TagService,
    val unitService: UnitService,
    val ingredientService: IngredientService,
    val applicationContext: ApplicationContext,
    private val mongoTemplate: MongoTemplate
) {

    private fun self(): MealService = applicationContext.getBean(MealService::class.java)


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

    @Cacheable("allMeals")
    fun findAll(): List<MealCardDto> {
        val aggregation = newAggregation(
            *AggregationUtils.getMealCardProjectionStages().toTypedArray()
        )

        return mongoTemplate.aggregate(aggregation, "meals", MealCardDto::class.java).mappedResults
    }
    // ... existing methods ...

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

    @Caching(
        evict = [
            CacheEvict(cacheNames = ["meals"], key = "#id"),
            CacheEvict(cacheNames = ["allMeals"], allEntries = true)
        ]
    )
    fun deleteById(id: ObjectId) {
        val meal = self().findById(id)
        meal.images?.forEach { image -> image.delete(image) }

        ratingService.deleteRatingsByMeal(meal)
        repository.deleteById(id)
    }

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

// Private projection classes to handle mapping without @DocumentReference
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

private data class MealIngredientProjection(
    val name: String,
    val amount: String,
    val ingredient: IngredientProjection?,
    val unit: IngredientUnit?
)

private data class IngredientProjection(
    var id: ObjectId,
    val name: String,
    val category: String,
    val unit: ObjectId
)


