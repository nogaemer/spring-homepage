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
import org.springframework.data.mongodb.core.aggregation.ArrayOperators.*
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators.Eq
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.`when`
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class UnifiedMealSearchService(
    private val mongoTemplate: MongoTemplate,
) {
    fun search(request: UnifiedMealSearchRequest): UnifiedMealSearchResponse {
        val stages = mutableListOf<AggregationOperation>()

        // ----------------------------
        // 1) Base match (tags + time)
        // ----------------------------
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
        // 2) Name filter + relevance score (token scoring like your FilterService.searchByName)
        // ----------------------------
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

            // score = sum(name matches token ? 1 : 0)
            var scoreExpression: AggregationExpression? = null
            for (token in tokens) {
                val matchCondition = StringOperators.valueOf("name")
                    .regexMatch(Pattern.quote(token))
                    .options("i")

                val scoreValue = `when`(matchCondition)
                    .then(1)
                    .otherwise(0)

                scoreExpression = if (scoreExpression == null) scoreValue
                else ArithmeticOperators.Add.valueOf(scoreExpression).add(scoreValue)
            }

            stages += Aggregation.match(Criteria().andOperator(*baseCriteria.toTypedArray()))
            stages += Aggregation.addFields().addField("relevanceScore").withValue(scoreExpression!!).build()
        } else {
            stages += Aggregation.match(Criteria().andOperator(*baseCriteria.toTypedArray()))
        }

        // ----------------------------
        // 3) Ingredient match (optional)
        // Your meals store ingredient references in MealIngredient.ingredient [file:63],
        // and your current filter compares against "$$ing.ingredient" [file:70].
        // ----------------------------
        val didYouMean = mutableMapOf<String, List<String>>()
        val resolvedIngredientIdsAsStrings = mutableListOf<String>()

        val ingredientNames = request.ingredients
            ?: emptyList()

        if (ingredientNames.isNotEmpty()) {
            // Resolve names -> ids by loading all ingredients once.
            // (For large DBs: build a normalizedName field + index later.)

            stages += Aggregation.addFields()
                .addField("matchingIngredients")
                .withValue(
                    Filter.filter("ingredients")
                        .`as`("ing")
                        .by(
                            // Check if the current ingredient's name ($$ing.name) is in your provided list.
                            // We use "\$\$" to escape the dollar signs in the Kotlin string template.
                            In.arrayOf(ingredientNames)
                                .containsValue("\$\$ing.ingredient")
                        )
                ).build()

            // Avoid divide-by-zero if a meal has 0 ingredients
            stages += Aggregation.addFields()
                .addField("ingSize").withValue(ingredientNames.size)
                .addField("matchSize").withValue(Size.lengthOfArray("matchingIngredients"))
                .build()

            stages += Aggregation.addFields().addField("matchingRatio").withValue(
                ArithmeticOperators.Divide.valueOf("matchSize").divideBy("ingSize")
            ).build()

            val minMatch = request.minIngredientMatch ?: 0.0
            if (minMatch > 0.0) {
                stages += Aggregation.match(Criteria.where("matchingRatio").gte(minMatch))
            }
        }

        // ----------------------------
        // 4) Ratings filter (optional)
        // Lookup ratings only if requested (userIds/minUserRating/requireUserRatingMatch)
        // ----------------------------
        val userIds = request.userIds
            ?.mapNotNull { runCatching { ObjectId(it) }.getOrNull() }
            ?: emptyList()

        println(userIds)

        val needsRatingLookup = userIds.isNotEmpty() || request.minUserRating != null || request.requireUserRatingMatch

        if (needsRatingLookup) {

            // Filter userRatings by userIds (if provided)
            if (userIds.isNotEmpty()) {
                // Build an $expr : { $in: ["$$rating.userId", userIds] }
                val expr = In.arrayOf(userIds).containsValue("\$userId")
                val matchOp = Aggregation.match(EvaluationOperators.Expr.valueOf(expr))

                stages += Aggregation.lookup()
                    .from("ratings")
                    .localField("_id")
                    .foreignField("mealId")
                    .pipeline(matchOp)        // only ratings whose userId is in userIds
                    .`as`("userRatings")

                // averageUserRating = (size==0 ? 0 : avg(userRatings.rating))
                val avgExpr = `when`(
                    Eq.valueOf(
                        Size.lengthOfArray("\$userRatings")
                    ).equalToValue(0)
                ).then(0)
                    .otherwise(Avg.avgOf("\$userRatings.rating"))

                stages += Aggregation.addFields().addFieldWithValue("averageUserRating", avgExpr).build()

            }

            // Filter userRatings by minUserRating (if provided)
            val minUserRating = request.minUserRating
            if (minUserRating != null) {
                val ratingFieldName = if (userIds.isNotEmpty()) "averageUserRating" else "rating"
                stages += Aggregation.match(Criteria.where(ratingFieldName).gte(minUserRating))
            }

            if (request.requireUserRatingMatch) {
                val ratingsFieldName = if (userIds.isNotEmpty()) "userRatings" else "ratings"
                stages += Aggregation.match(Criteria.where("$ratingsFieldName.0").exists(true))
            }
        }

        // ----------------------------
        // 5) Sorting + paging
        // ----------------------------
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

        if (request.skip > 0) stages += Aggregation.skip(request.skip)
        stages += Aggregation.limit(request.limit.toLong())

        // ----------------------------
        // 6) Project to MealCardDto (reuse your existing helper)
        // If you want matchingIngredients included, pass it like you already do [file:70].
        // ----------------------------
        val projectionStages = if (ingredientNames.isNotEmpty()) {
            AggregationUtils.getMealCardProjectionStages("matchingIngredients")
        } else {
            AggregationUtils.getMealCardProjectionStages()
        }
        stages.addAll(projectionStages)

        val pipeline = Aggregation.newAggregation(*stages.toTypedArray())
        val results = mongoTemplate.aggregate(pipeline, "meals", MealCardDto::class.java).mappedResults

        println("Aggregation pipeline: $pipeline")

        return UnifiedMealSearchResponse(
            results = results
        )
    }
}