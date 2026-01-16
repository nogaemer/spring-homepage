package de.nogaemer.springhomepage.main.filters

import de.nogaemer.springhomepage.main.meals.dto.MealCardDto
import de.nogaemer.springhomepage.main.meals.dto.UnifiedMealSearchRequest.SortBy
import de.nogaemer.springhomepage.main.utils.AggregationUtils
import de.nogaemer.springhomepage.user.UserRepository
import de.nogaemer.springhomepage.user.UserResponse
import de.nogaemer.springhomepage.user.UserService
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import java.util.regex.Pattern

/**
 * Service for advanced meal filtering and search operations.
 *
 * Provides sophisticated MongoDB aggregation-based filtering including:
 * - Ingredient-based matching with configurable thresholds
 * - User-specific favorite meals by rating
 * - Name-based search with tokenized scoring
 * - User lookup with name filtering
 *
 * ## Search & Filter Features
 * - **Ingredient Matching**: Finds meals containing specified ingredients with minimum match ratio
 * - **Favorites**: Retrieves user's highly-rated meals (default: 4+ stars)
 * - **Name Search**: Token-based fuzzy search with relevance scoring
 * - **User Filters**: Returns available users for filtering meal creators
 *
 * ## Performance Optimization
 * All search operations use MongoDB aggregation pipelines for:
 * - Server-side filtering and scoring
 * - Efficient lookup joins to avoid N+1 queries
 * - Projection to lightweight DTOs
 *
 * @property userRepository Repository for user data access
 * @property userService Service for current user context
 * @property mongoTemplate MongoDB template for aggregation pipeline execution
 *
 * @see FilterController
 * @see MealCardDto
 * @see AggregationUtils
 */
@Service
class FilterService(
    val userRepository: UserRepository,
    val userService: UserService,
    private val mongoTemplate: MongoTemplate
) {

    /**
     * Retrieves all available filter options for the UI.
     *
     * Returns comprehensive filter metadata including all registered users
     * and available sort parameters with their display names.
     *
     * ## Response Contents
     * - **users**: List of all users with ID and name for "created by" filtering
     * - **sortParameters**: Available sort options (relevance, name, rating, etc.)
     *
     * ## Sort Parameters
     * Includes all options from [SortBy] enum with:
     * - id: Internal enum name
     * - name: User-friendly display label
     * - selected: True for default option (RELEVANCE)
     *
     * @return [FilterResponse] containing all filter options
     */
    fun getFilters(): FilterResponse {
        val users = userRepository.findAll().map { UserResponse(it.id!!, it.name) }
        val sortParameters = SortBy.entries.map {
            SortParameter(
                it.name,
                it.value,
                (it == SortBy.RELEVANCE)
            )
        }

        return FilterResponse(users, sortParameters)
    }

    /**
     * Searches for users by name with optional filtering.
     *
     * ## Search Behavior
     * - **No filter**: Returns all users in the system
     * - **With name**: Case-insensitive regex match on user names
     *
     * ## MongoDB Strategy
     * Uses aggregation pipeline for filtered queries:
     * 1. Match stage with regex pattern (case-insensitive)
     * 2. Project only required fields (name, id)
     *
     * ## Use Cases
     * - Autocomplete for "created by" filters
     * - User selection in meal creation/editing
     * - Admin user management interfaces
     *
     * @param name Optional search term for filtering user names (case-insensitive)
     * @return List of [UserResponse] objects matching the search criteria
     */
    fun getUsers(name: String? = null): List<UserResponse> {
        val users = if (name.isNullOrBlank()) {
            userRepository.findAll().map { UserResponse(it.id!!, it.name) }
        } else {
            val stages = mutableListOf<AggregationOperation>()
            val pattern = Pattern.quote(name.trim())
            stages.add(match(Criteria.where("name").regex(pattern, "i")))
            stages.add(project("name", "id"))

            val pipeline = newAggregation(*stages.toTypedArray())
            mongoTemplate.aggregate(pipeline, "users", UserResponse::class.java).mappedResults
        }
        return users
    }


    /**
     * Retrieves meals that the user has favorited based on their ratings.
     *
     * Queries the ratings collection to find meals that the current user has
     * rated at or above the specified threshold, then enriches with full meal details.
     *
     * ## Aggregation Pipeline
     * 1. **Match**: Filter ratings by userId and minimum rating threshold
     * 2. **Lookup**: Join with meals collection via mealId
     * 3. **Unwind**: Flatten the joined meal array
     * 4. **Replace Root**: Promote meal to top-level document
     * 5. **Project**: Transform to [MealCardDto] with ingredient/unit lookups
     *
     * ## Default Behavior
     * - Uses current authenticated user from [UserService]
     * - Default minimum rating: 4 stars (out of 5)
     * - Returns full meal cards with images and ingredient details
     *
     * ## Performance Note
     * Query starts from ratings collection (typically smaller) rather than
     * scanning all meals, improving performance for users with many favorites.
     *
     * @param userId User's ObjectId (defaults to current authenticated user)
     * @param minRating Minimum rating threshold (defaults to 4, range: 1-5)
     * @return List of [MealCardDto] for favorited meals sorted by rating
     */
    fun getMyFavoriteMeals(
        userId: ObjectId = userService.getCurrentUser().id!!,
        minRating: Int = 4
    ): List<MealCardDto> {

        val stages = mutableListOf<AggregationOperation>()
        stages.add(match(Criteria.where("userId").`is`(userId).and("rating").gte(minRating)))
        stages.add(lookup("meals", "mealId", "_id", "meal"))
        stages.add(unwind("meal"))
        stages.add(replaceRoot("meal"))
        stages.addAll(AggregationUtils.getMealCardProjectionStages())

        val pipeline = newAggregation(
            *stages.toTypedArray()
        )

        return mongoTemplate.aggregate(
            pipeline,
            "ratings",
            MealCardDto::class.java
        ).mappedResults
    }


    /**
     * Finds meals that can be prepared with specified ingredients.
     *
     * Implements intelligent ingredient matching with configurable threshold,
     * calculating how many of a meal's required ingredients are available.
     *
     * ## Algorithm
     * 1. **Filter**: For each meal, identify which ingredients match the provided list
     * 2. **Calculate Ratio**: matchingIngredients / totalIngredients
     * 3. **Threshold**: Keep only meals with ratio >= minMatch
     * 4. **Sort**: Order by matching ratio (best matches first)
     *
     * ## Match Ratio Examples
     * - Meal needs [A, B, C], user has [A, B] → ratio = 2/3 = 0.67
     * - Meal needs [X, Y], user has [X, Y, Z] → ratio = 2/2 = 1.0 (perfect match)
     *
     * ## MongoDB Operations
     * Uses advanced aggregation with:
     * - **$filter**: Find matching ingredients within meal's ingredient array
     * - **$size**: Count matching vs total ingredients
     * - **$divide**: Calculate match percentage
     * - Dollar sign escaping (\$\$) for nested variables in Kotlin templates
     *
     * ## Use Case
     * "What can I cook with what I have?" feature - helps users minimize
     * shopping trips by suggesting meals with highest ingredient overlap.
     *
     * @param ingredients List of ingredient ObjectId strings available to the user
     * @param minMatch Minimum match ratio required (0.0-1.0, e.g., 0.5 = 50% match)
     * @return List of [MealCardDto] sorted by match ratio (descending), includes matchingRatio field
     */
    fun getByIngredients(ingredients: List<String>, minMatch: Double): List<MealCardDto> {

        val stages = mutableListOf<AggregationOperation>()

        // 1. Calculate 'matchingIngredients' by filtering the meal's ingredients
        stages.add(
            addFields()
                .addField("matchingIngredients")
                .withValue(
                    ArrayOperators.Filter.filter("ingredients")
                        .`as`("ing")
                        .by(
                            // Check if the current ingredient's name ($$ing.name) is in your provided list.
                            // We use "\$\$" to escape the dollar signs in the Kotlin string template.
                            ArrayOperators.In.arrayOf(ingredients)
                                .containsValue(Document("\$toString", "\$\$ing.ingredient"))
                        )
                ).build()
        )

        // 2. Calculate 'matchingRatio': size(matches) / size(total)
        stages.add(
            addFields()
                .addField("matchingRatio")
                .withValue(
                    ArithmeticOperators.Divide.valueOf(
                        ArrayOperators.Size.lengthOfArray("matchingIngredients")
                    ).divideBy(
                        ArrayOperators.Size.lengthOfArray("ingredients")
                    )
                ).build()
        )

        // 3. Filter meals that meet the minimum match percentage (e.g., 0.5 for 50%)
        stages.add(match(Criteria.where("matchingRatio").gte(minMatch)))

        stages.add(sort(Sort.by(Sort.Direction.DESC, "matchingRatio")))

        // 4. Optimization: Lookup related data to avoid N+1 queries & 5. Project to DTO structure
        stages.addAll(AggregationUtils.getMealCardProjectionStages("matchingIngredients"))

        val pipeline = newAggregation(
            *stages.toTypedArray()
        )

        return mongoTemplate.aggregate(pipeline, "meals", MealCardDto::class.java).mappedResults
    }

    /**
     * Searches meals by name with intelligent multi-token matching and scoring.
     *
     * Implements fuzzy search with relevance ranking based on how many search
     * terms match the meal name. Handles multi-word queries intelligently.
     *
     * ## Search Algorithm
     * 1. **Tokenization**: Split query into individual words (whitespace-separated)
     * 2. **Initial Filter**: Match meals containing at least one token (optimization)
     * 3. **Scoring**: Count how many tokens match each meal (more matches = higher score)
     * 4. **Sort**: Order by score descending (best matches first)
     *
     * ## Search Examples
     * - Query: "chocolate cake"
     *   - "Chocolate Lava Cake" → score 2 (both tokens match)
     *   - "Chocolate Chip Cookies" → score 1 (one token matches)
     *   - "Vanilla Cake" → score 1 (one token matches)
     *
     * ## Case Insensitivity
     * All matching is case-insensitive via MongoDB regex "i" option.
     *
     * ## Empty Query Handling
     * Returns empty list for null, blank, or whitespace-only queries to
     * avoid unnecessary database operations.
     *
     * ## MongoDB Strategy
     * Uses $regexMatch for pattern matching and conditional operators to
     * accumulate scores, all performed server-side for efficiency.
     *
     * @param query Search string (can contain multiple words)
     * @return List of [MealCardDto] ordered by relevance score (most relevant first)
     */
    fun searchByName(query: String?): List<MealCardDto> {
        if (query.isNullOrBlank()) return emptyList()

        val tokenizedQuery = query.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }

        if (tokenizedQuery.isEmpty()) return emptyList()

        // 1. Filter: At least one token must match (optimization)
        val orCriteria = Criteria().orOperator(
            *tokenizedQuery.map { token ->
                Criteria.where("name").regex(Pattern.quote(token), "i")
            }.toTypedArray()
        )

        // 2. Score: Count how many tokens match
        var scoreExpression: AggregationExpression? = null

        for (token in tokenizedQuery) {
            // Check if name matches the token (case-insensitive)
            val matchCondition = StringOperators.valueOf("name")
                .regexMatch(Pattern.quote(token))
                .options("i")

            // Convert boolean match to 1 or 0
            val scoreValue = ConditionalOperators.`when`(matchCondition)
                .then(1)
                .otherwise(0)

            // Sum up the scores
            scoreExpression = if (scoreExpression == null) {
                scoreValue
            } else {
                ArithmeticOperators.Add.valueOf(scoreExpression).add(scoreValue)
            }
        }

        val stages = mutableListOf<AggregationOperation>()
        stages.add(match(orCriteria))
        stages.add(
            addFields()
                .addField("score")
                .withValue(scoreExpression!!)
                .build()
        )
        stages.add(sort(Sort.Direction.DESC, "score"))
        stages.addAll(AggregationUtils.getMealCardProjectionStages())

        val pipeline = newAggregation(
            *stages.toTypedArray()
        )

        return mongoTemplate.aggregate(pipeline, "meals", MealCardDto::class.java).mappedResults
    }





}
