/**
 * Advanced meal search service with unified multi-criteria filtering and relevance scoring.
 *
 * This service provides a sophisticated search and filtering system for meals using MongoDB
 * aggregation pipelines. It supports:
 * - Text search with token-based relevance scoring
 * - Ingredient matching with ratio-based filtering
 * - Time range filtering
 * - Tag-based filtering
 * - User-specific rating filtering
 * - Multiple sorting strategies
 * - Weighted relevance scoring combining all criteria
 *
 * ## Architecture
 * The service builds a dynamic MongoDB aggregation pipeline based on the search request,
 * adding stages conditionally to optimize performance. This approach:
 * - Pushes filtering to the database layer
 * - Minimizes data transfer
 * - Leverages MongoDB indexes
 * - Supports complex scoring algorithms
 *
 * ## Scoring Algorithm
 * The relevance score combines multiple weighted factors:
 * - **Name Match (30%)**: Token-based text matching
 * - **Ingredient Match (25%)**: Ratio of requested ingredients present
 * - **User Rating (20%)**: User-specific or average ratings
 * - **Tag Match (10%)**: Overlap with requested tags
 * - **Time Proximity (10%)**: Distance from time range midpoint
 * - **Overall Rating (5%)**: Global meal rating
 *
 * ## Performance Characteristics
 * - Uses aggregation pipelines (no in-memory post-processing)
 * - Efficient early filtering reduces document count
 * - Sort optimization via compound scoring
 * - Pagination support via skip/limit
 * - No N+1 query issues (single pipeline execution)
 *
 * @property mongoTemplate MongoDB template for executing aggregation queries
 * @see UnifiedMealSearchRequest
 * @see UnifiedMealSearchResponse
 * @see AggregationUtils
 */
package de.nogaemer.springhomepage.main.meals

import de.nogaemer.springhomepage.main.meals.dto.MealCardDto
import de.nogaemer.springhomepage.main.meals.dto.UnifiedMealSearchRequest
import de.nogaemer.springhomepage.main.meals.dto.UnifiedMealSearchResponse
import de.nogaemer.springhomepage.main.utils.AggregationUtils
import org.bson.types.ObjectId
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators.Avg
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators.Min
import org.springframework.data.mongodb.core.aggregation.Aggregation.addFields
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.*
import org.springframework.data.mongodb.core.aggregation.ArrayOperators.*
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators.Eq
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.IfNull.ifNull
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.`when`
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import java.util.regex.Pattern
import kotlin.math.max

/**
 * Service providing unified meal search with multi-criteria filtering and relevance scoring.
 *
 * Implements a sophisticated search algorithm that combines text matching, ingredient
 * filtering, rating constraints, and relevance scoring in a single optimized query.
 */
@Service
class UnifiedMealSearchService(
    private val mongoTemplate: MongoTemplate,
) {

    /**
     * Executes a unified meal search with multi-criteria filtering and relevance scoring.
     *
     * Builds and executes a MongoDB aggregation pipeline that:
     * 1. Filters meals by base criteria (tags, time range)
     * 2. Applies name-based text matching with token scoring
     * 3. Filters by ingredient presence with match ratio calculation
     * 4. Optionally filters by user-specific ratings
     * 5. Calculates weighted relevance score
     * 6. Sorts by chosen criteria (relevance, rating, time, etc.)
     * 7. Applies pagination (skip/limit)
     * 8. Projects to MealCardDto format
     *
     * ## Pipeline Stage Breakdown
     *
     * ### Stage 1: Base Match (Tags + Time)
     * Filters meals that match ALL of:
     * - Time between minTime and maxTime
     * - If tagIds provided: meal must have at least one matching tag
     * - MongoDB Index: Compound index on (tags, time) recommended
     *
     * ### Stage 2: Name Filter + Relevance Score
     * If name tokens provided:
     * - Filters meals matching ANY token (OR logic)
     * - Adds `nameScore` field = sum of matching tokens
     * - Uses case-insensitive regex matching
     * - MongoDB Index: Text index on name recommended for better performance
     *
     * ### Stage 3: Ingredient Match (Optional)
     * If ingredients provided:
     * - Filters ingredients array to matching elements
     * - Calculates `matchingRatio` = matchCount / requestedCount
     * - Filters meals by minimum match ratio if specified
     * - MongoDB Performance: Uses $filter array operator (no lookup required)
     *
     * ### Stage 4: User Rating Lookup (Optional)
     * If userIds or rating filters provided:
     * - Performs $lookup join with ratings collection
     * - Filters ratings by requested userIds
     * - Calculates average user rating
     * - Filters by minimum rating if specified
     * - Filters to only rated meals if requireUserRating=true
     * - MongoDB Index: Index on (ratings.mealId, ratings.userId) recommended
     *
     * ### Stage 5: Relevance Score Calculation
     * Calculates weighted score combining:
     * - nameScore: (matching tokens / total tokens) * 30%
     * - ingredientScore: matchingRatio * 25%
     * - userRatingScore: (avgUserRating / 5) * 20%
     * - tagScore: (matching tags / requested tags) * 10%
     * - timeScore: proximity to time range midpoint * 10%
     * - globalRatingScore: (rating / 5) * 5%
     *
     * ### Stage 6: Sorting
     * Supports multiple sort strategies:
     * - RELEVANCE: Sort by computed relevance score (default for multi-criteria)
     * - RATING: Sort by global meal rating
     * - TIME_ASC/DESC: Sort by preparation time
     * - INGREDIENT_MATCH: Sort by ingredient match ratio
     * - USER_AVG_RATING: Sort by user-specific average rating
     *
     * ### Stage 7: Pagination
     * - skip: Offset into result set
     * - limit: Maximum results to return
     *
     * ### Stage 8: Projection
     * Projects to MealCardDto using AggregationUtils, including:
     * - Basic meal info (id, name, time, rating, images)
     * - Tag lookup and projection
     * - Optional matching ingredients array
     *
     * ## Request Parameters
     *
     * @param request.name Optional search text (split into tokens, OR logic)
     * @param request.tagIds Optional list of tag ObjectId strings (meals must match at least one)
     * @param request.minTime Optional minimum preparation time in minutes (default: 0)
     * @param request.maxTime Optional maximum preparation time in minutes (default: unlimited)
     * @param request.ingredients Optional list of ingredient ObjectId strings to match
     * @param request.minIngredientMatch Optional minimum ingredient match ratio 0.0-1.0 (default: 0.0)
     * @param request.userIds Optional list of user ObjectId strings for rating filtering
     * @param request.minUserRating Optional minimum user rating 0.0-5.0
     * @param request.requireUserRating Optional require at least one user rating (default: false)
     * @param request.sortBy Sort strategy (default: RELEVANCE)
     * @param request.skip Pagination offset (default: 0)
     * @param request.limit Maximum results (default: 20)
     *
     * ## Response Format
     * Returns UnifiedMealSearchResponse containing:
     * - results: List<MealCardDto> with all meal card information
     *
     * ## Performance Considerations
     * - Pipeline executes in database (no N+1 queries)
     * - Recommend indexes:
     *   - meals: (tags, time), (name text), (_id)
     *   - ratings: (mealId, userId)
     *   - ingredients: (_id)
     * - Large ingredient lists may impact performance (array filtering)
     * - User rating lookup adds join overhead (skip if not needed)
     *
     * ## Example Usage
     * ```kotlin
     * val request = UnifiedMealSearchRequest(
     *     name = "pasta carbonara",
     *     tagIds = listOf("tag1", "tag2"),
     *     minTime = 15,
     *     maxTime = 45,
     *     ingredients = listOf("ing1", "ing2", "ing3"),
     *     minIngredientMatch = 0.5,  // Must have 50% of requested ingredients
     *     sortBy = SortBy.RELEVANCE,
     *     limit = 20
     * )
     * val response = unifiedMealSearchService.search(request)
     * ```
     *
     * @param request Search request with filtering and sorting criteria
     * @return UnifiedMealSearchResponse containing matching meals as MealCardDto objects
     * @throws Exception if aggregation pipeline execution fails
     */
    fun search(request: UnifiedMealSearchRequest): UnifiedMealSearchResponse {
        val stages = mutableListOf<AggregationOperation>()

        // ----------------------------
        // Stage 1: Base Match - Filter by tags and time range
        // ----------------------------
        // This stage applies the most restrictive filters first to minimize
        // the number of documents processed in subsequent stages.
        val timeMin = request.minTime ?: 0L
        val timeMax = request.maxTime ?: Long.MAX_VALUE

        val baseCriteria = mutableListOf(
            Criteria.where("time").gte(timeMin).lte(timeMax)
        )

        val tagObjectIds = request.tagIds
            ?.mapNotNull { runCatching { ObjectId(it) }.getOrNull() }
            ?: emptyList()

        if (tagObjectIds.isNotEmpty()) {
            baseCriteria += Criteria.where("tags").`in`(tagObjectIds)
        } else {
            baseCriteria += Criteria.where("tags").exists(true)
        }

        // ----------------------------
        // Stage 2: Name Filter + Token-Based Relevance Score
        // ----------------------------
        // Splits the search query into tokens and filters meals matching ANY token.
        // Calculates a nameScore based on how many tokens match the meal name.
        // Example: "pasta carbonara" -> tokens ["pasta", "carbonara"]
        //          Meal with name "Pasta with Bacon" matches 1 token -> score = 1
        val tokens = request.name
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        if (tokens.isNotEmpty()) {
            val orCriteria = Criteria().orOperator(
                *tokens.map { token ->
                    Criteria.where("name").regex(Pattern.quote(token), "i")
                }.toTypedArray()
            )
            baseCriteria += orCriteria

            // Build nameScore expression: sum(name matches token ? 1 : 0) for each token
            // This counts how many search tokens are present in the meal name
            var scoreExpression: AggregationExpression? = null
            for (token in tokens) {
                val matchCondition = StringOperators.valueOf("name")
                    .regexMatch(Pattern.quote(token))
                    .options("i")

                val scoreValue = `when`(matchCondition)
                    .then(1)
                    .otherwise(0)

                scoreExpression = if (scoreExpression == null) scoreValue
                else Add.valueOf(scoreExpression).add(scoreValue)
            }

            stages += Aggregation.match(Criteria().andOperator(*baseCriteria.toTypedArray()))
            stages += addFields().addField("nameScore").withValue(scoreExpression!!).build()
        } else {
            stages += Aggregation.match(Criteria().andOperator(*baseCriteria.toTypedArray()))
        }

        // ----------------------------
        // Stage 3: Ingredient Match with Ratio Calculation
        // ----------------------------
        // Filters the ingredients array to only matching ingredients, then calculates
        // what percentage of requested ingredients are present in the meal.
        // This enables queries like "find meals with at least 50% of these ingredients".

        val ingredientNames = request.ingredients
            ?: emptyList()

        if (ingredientNames.isNotEmpty()) {
            // Filter ingredients array to only those matching the requested ingredient ObjectIds
            // Uses $filter operator: keep only ingredients where $$ing.ingredient is in the request list
            stages += addFields()
                .addField("matchingIngredients")
                .withValue(
                    Filter.filter("ingredients")
                        .`as`("ing")
                        .by(
                            // Check if the ingredient's ObjectId ($$ing.ingredient) is in the requested list
                            // Double-dollar signs ($$) reference the filter variable in MongoDB
                            In.arrayOf(ingredientNames)
                                .containsValue("\$\$ing.ingredient")
                        )
                ).build()

            // Calculate match ratio to determine ingredient coverage
            // Avoid divide-by-zero by ensuring we have valid counts
            stages += addFields()
                .addField("ingSize").withValue(ingredientNames.size)
                .addField("matchSize").withValue(Size.lengthOfArray("matchingIngredients"))
                .build()

            // matchingRatio = matchSize / ingSize
            // This gives us a 0.0-1.0 value representing ingredient coverage

            stages += addFields().addField("matchingRatio").withValue(
                Divide.valueOf("matchSize").divideBy("ingSize")
            ).build()

            val minMatch = request.minIngredientMatch ?: 0.0
            if (minMatch > 0.0) {
                stages += Aggregation.match(Criteria.where("matchingRatio").gte(minMatch))
            }
        }

        // ----------------------------
        // Stage 4: User Rating Lookup and Filtering
        // ----------------------------
        // Optionally joins with the ratings collection to:
        // - Filter ratings by specific user IDs
        // - Calculate user-specific average ratings
        // - Filter meals by minimum rating threshold
        // - Require at least one rating from specified users
        val userIds = request.userIds
            ?.mapNotNull { runCatching { ObjectId(it) }.getOrNull() }
            ?: emptyList()

        println(userIds)

        val needsRatingLookup = userIds.isNotEmpty() || request.minUserRating != null || request.requireUserRating

        if (needsRatingLookup) {

            // Perform $lookup with ratings collection, filtering by user IDs if provided
            // This joins ratings.mealId with meals._id and filters within the lookup pipeline
            if (userIds.isNotEmpty()) {
                // Build $expr filter to check if rating.userId is in the requested user list
                // This filters the ratings collection before joining
                val expr = In.arrayOf(userIds).containsValue("\$userId")
                val matchOp = Aggregation.match(EvaluationOperators.Expr.valueOf(expr))

                stages += Aggregation.lookup()
                    .from("ratings")
                    .localField("_id")
                    .foreignField("mealId")
                    .pipeline(matchOp)        // Filter: only include ratings from specified users
                    .`as`("userRatings")

                // Calculate average rating from user-filtered ratings
                // Handle empty array case (no ratings) by defaulting to 0
                val avgExpr = `when`(
                    Eq.valueOf(
                        Size.lengthOfArray("\$userRatings")
                    ).equalToValue(0)
                ).then(0)
                    .otherwise(Avg.avgOf("\$userRatings.rating"))

                stages += addFields().addFieldWithValue("averageUserRating", avgExpr).build()

            }

            // Apply minimum rating filter if specified
            // Uses the appropriate field name based on whether user filtering was applied
            val minUserRating = request.minUserRating
            if (minUserRating != null) {
                val ratingFieldName = if (userIds.isNotEmpty()) "averageUserRating" else "rating"
                stages += Aggregation.match(Criteria.where(ratingFieldName).gte(minUserRating))
            }

            // Require at least one rating if specified
            // Checks that the ratings array has at least one element (ratings.0 exists)

            if (request.requireUserRating) {
                val ratingsFieldName = if (userIds.isNotEmpty()) "userRatings" else "ratings"
                stages += Aggregation.match(Criteria.where("$ratingsFieldName.0").exists(true))
            }
        }

        // ----------------------------
        // Stage 5: Weighted Relevance Score Calculation
        // ----------------------------
        // Combines all scoring factors into a single relevance score using weighted formula:
        // relevance = (nameScore * 0.30) + (ingredientMatch * 0.25) + (userRating * 0.20)
        //           + (tagMatch * 0.10) + (timeProximity * 0.10) + (globalRating * 0.05)
        //
        // This allows sorting by overall relevance across multiple criteria.
        val nameWeight = 0.30
        val ingredientWeight = 0.25
        val userWeight = 0.20
        val tagWeight = 0.10
        val timeWeight = 0.10
        val ratingWeight = 0.05

        // Calculate normalization factors (avoid division by zero)

        val tokenCount = if (tokens.isEmpty()) 1 else tokens.size // avoid divide by zero
        val tagCount = tagObjectIds.size

        // Calculate time range midpoint and range for proximity scoring
        val midTime = timeMin.toDouble() + (timeMax - timeMin).toDouble() / 2.0
        val timeRange = max(1.0, (timeMax - timeMin).toDouble())

        // Component 1: Name Score - Normalized by token count, weighted at 30%
        // Formula: (matchingTokens / totalTokens) * nameWeight
        val nameScore: AggregationExpression = Multiply.valueOf(
            Divide.valueOf(
                ifNull("nameScore").then(0)
            ).divideBy(tokenCount)
        ).multiplyBy(nameWeight)

        // Component 2: Ingredient Score - Match ratio weighted at 25%
        // Formula: matchingRatio * ingredientWeight
        // matchingRatio already computed as (matchCount / requestedCount)
        val ingredientScore: AggregationExpression = Multiply.valueOf(
            ifNull("matchingRatio").then(0)
        ).multiplyBy(ingredientWeight)

        // Component 3: Global Rating Score - Normalized to 0-1 scale, weighted at 5%
        // Formula: (rating / 5) * ratingWeight
        val ratingScore: AggregationExpression = Multiply.valueOf(
            Divide.valueOf(
                ifNull("rating").then(0)
            ).divideBy(5)
        ).multiplyBy(ratingWeight)

        // Component 4: Tag Score - Set intersection ratio, weighted at 10%
        // Formula: if tagCount > 0 then (matchingTags / requestedTags) * tagWeight else 0
        // Uses MongoDB $setIntersection to find common tags
        val requestedTagsValue = arrayOf(tagObjectIds).toObject()
        val setIntersectionExpr = SetOperators.SetIntersection.arrayAsSet("tags").intersects(requestedTagsValue)
        val tagFractionExpr: AggregationExpression = Divide.valueOf(
            Size.lengthOfArray(setIntersectionExpr)
        ).divideBy(tagCount.takeIf { it > 0 } ?: 1)

        val tagScore: AggregationExpression = if (tagCount > 0) {
            Multiply.valueOf(tagFractionExpr).multiplyBy(tagWeight)
        } else {
            // No tags requested, score is 0 (constant expression)
            Multiply.valueOf(Divide.valueOf(ifNull("rating").then(0)).divideBy(1)).multiplyBy(0.0)
        }

        // Component 5: Time Score - Proximity to time range midpoint, weighted at 10%
        // Formula: (1 - min(|time - midTime| / timeRange, 1)) * timeWeight
        // Meals closer to the midpoint score higher
        // Calculation: abs(time - midTime) -> normalize by range -> clamp to [0,1] -> invert

        val diffExpr = Abs.absoluteValueOf(Subtract.valueOf("time").subtract(midTime))
        val normalizedDiffExpr = Divide.valueOf(diffExpr).divideBy(timeRange)
        val clampedExpr = Min.minOf(normalizedDiffExpr).and(LiteralOperators.valueOf(1.0).asLiteral())
        val timeScore: AggregationExpression = Multiply.valueOf(
            Subtract.valueOf(LiteralOperators.valueOf(1.0).asLiteral()).subtract(clampedExpr)
        ).multiplyBy(timeWeight)

        // Component 6: User Rating Score - User-specific average rating, weighted at 20%
        // Formula: (avgUserRating / 5) * userWeight
        // Falls back to global rating if no user rating available
        val avgOrRatingExpr = ifNull("averageUserRating").then(ifNull("rating").then(0))
        val userAvgScore: AggregationExpression = Multiply.valueOf(
            Divide.valueOf(avgOrRatingExpr).divideBy(5)
        ).multiplyBy(userWeight)

        // Sum all weighted components to get final relevance score
        // This single score allows sorting by overall relevance across all criteria
        val relevanceExpr = Add.valueOf(nameScore)
            .add(ingredientScore)
            .add(ratingScore)
            .add(tagScore)
            .add(timeScore)
            .add(userAvgScore)

        // Add the computed relevance score as a new field in the pipeline
        // This field can then be used for sorting and filtering
        stages.add(
            addFields().addField("relevanceScore").withValue(relevanceExpr).build()
        )


        // ----------------------------
        // Stage 6: Sorting Strategy Selection
        // ----------------------------
        // Applies the requested sort order. Multiple sort fields provide tie-breaking.
        // RELEVANCE sort uses the computed relevance score from previous stage.
        val sortStage = when (request.sortBy) {
            UnifiedMealSearchRequest.SortBy.RELEVANCE ->
                Aggregation.sort(Sort.by(Sort.Order.desc("relevanceScore"), Sort.Order.desc("rating")))

            UnifiedMealSearchRequest.SortBy.RATING ->
                Aggregation.sort(Sort.by(Sort.Order.desc("rating")))

            UnifiedMealSearchRequest.SortBy.TIME_ASC ->
                Aggregation.sort(Sort.by(Sort.Order.asc("time")))

            UnifiedMealSearchRequest.SortBy.TIME_DESC ->
                Aggregation.sort(Sort.by(Sort.Order.desc("time")))

            UnifiedMealSearchRequest.SortBy.INGREDIENT_MATCH ->
                Aggregation.sort(Sort.by(Sort.Order.desc("matchingRatio"), Sort.Order.desc("rating")))

            UnifiedMealSearchRequest.SortBy.USER_AVG_RATING ->
                Aggregation.sort(Sort.by(Sort.Order.desc("averageUserRating"), Sort.Order.desc("rating")))
        }
        stages += sortStage

        // Stage 7: Pagination - Skip and limit for result set windowing
        if (request.skip > 0) stages += Aggregation.skip(request.skip)
        stages += Aggregation.limit(request.limit.toLong())

        // ----------------------------
        // Stage 8: Projection to MealCardDto
        // ----------------------------
        // Transforms the final result set into the MealCardDto format expected by clients.
        // Uses AggregationUtils helper for consistent projection logic.
        // If ingredients were filtered, includes the matchingIngredients array.
        val projectionStages = if (ingredientNames.isNotEmpty()) {
            AggregationUtils.getMealCardProjectionStages("matchingIngredients")
        } else {
            AggregationUtils.getMealCardProjectionStages()
        }
        stages.addAll(projectionStages)

        // Execute the aggregation pipeline against the meals collection
        val pipeline = Aggregation.newAggregation(*stages.toTypedArray())
        println(pipeline.toString())
        val results = mongoTemplate.aggregate(pipeline, "meals", MealCardDto::class.java).mappedResults

        println("Aggregation pipeline: $pipeline")

        return UnifiedMealSearchResponse(
            results = results
        )
    }
}