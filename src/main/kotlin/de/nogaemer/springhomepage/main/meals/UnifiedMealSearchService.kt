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
                else Add.valueOf(scoreExpression).add(scoreValue)
            }

            stages += Aggregation.match(Criteria().andOperator(*baseCriteria.toTypedArray()))
            stages += addFields().addField("nameScore").withValue(scoreExpression!!).build()
        } else {
            stages += Aggregation.match(Criteria().andOperator(*baseCriteria.toTypedArray()))
        }

        // ----------------------------
        // 3) Ingredient match (optional)
        // Your meals store ingredient references in MealIngredient.ingredient [file:63],
        // and your current filter compares against "$$ing.ingredient" [file:70].
        // ----------------------------

        val ingredientNames = request.ingredients
            ?: emptyList()

        if (ingredientNames.isNotEmpty()) {
            // Resolve names -> ids by loading all ingredients once.
            // (For large DBs: build a normalizedName field + index later.)

            stages += addFields()
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
            stages += addFields()
                .addField("ingSize").withValue(ingredientNames.size)
                .addField("matchSize").withValue(Size.lengthOfArray("matchingIngredients"))
                .build()

            stages += addFields().addField("matchingRatio").withValue(
                Divide.valueOf("matchSize").divideBy("ingSize")
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

        val needsRatingLookup = userIds.isNotEmpty() || request.minUserRating != null || request.requireUserRating

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

                stages += addFields().addFieldWithValue("averageUserRating", avgExpr).build()

            }

            // Filter userRatings by minUserRating (if provided)
            val minUserRating = request.minUserRating
            if (minUserRating != null) {
                val ratingFieldName = if (userIds.isNotEmpty()) "averageUserRating" else "rating"
                stages += Aggregation.match(Criteria.where(ratingFieldName).gte(minUserRating))
            }

            if (request.requireUserRating) {
                val ratingsFieldName = if (userIds.isNotEmpty()) "userRatings" else "ratings"
                stages += Aggregation.match(Criteria.where("$ratingsFieldName.0").exists(true))
            }
        }

        // kotlin
        // Calculate a weighted relevanceScore and add it to the aggregation pipeline
        val nameWeight = 0.30
        val ingredientWeight = 0.25
        val userWeight = 0.20
        val tagWeight = 0.10
        val timeWeight = 0.10
        val ratingWeight = 0.05

        val tokenCount = if (tokens.isEmpty()) 1 else tokens.size // avoid divide by zero
        val tagCount = tagObjectIds.size

        val midTime = timeMin.toDouble() + (timeMax - timeMin).toDouble() / 2.0
        val timeRange = max(1.0, (timeMax - timeMin).toDouble())

        // nameScore = (ifNull(score,0) / tokenCount) * nameWeight
        val nameScore: AggregationExpression = Multiply.valueOf(
            Divide.valueOf(
                ifNull("nameScore").then(0)
            ).divideBy(tokenCount)
        ).multiplyBy(nameWeight)

        // ingredientMatch = ifNull(matchingRatio, 0) * ingredientWeight
        val ingredientScore: AggregationExpression = Multiply.valueOf(
            ifNull("matchingRatio").then(0)
        ).multiplyBy(ingredientWeight)

        // ratingScore = (ifNull(rating,0) / 5) * ratingWeight
        val ratingScore: AggregationExpression = Multiply.valueOf(
            Divide.valueOf(
                ifNull("rating").then(0)
            ).divideBy(5)
        ).multiplyBy(ratingWeight)

        // tagScore:
        // if tagCount > 0 then (size(setIntersection(tags, requestedTags)) / tagCount) * tagWeight else 0
        val requestedTagsValue = arrayOf(tagObjectIds).toObject()
        val setIntersectionExpr = SetOperators.SetIntersection.arrayAsSet("tags").intersects(requestedTagsValue)
        val tagFractionExpr: AggregationExpression = Divide.valueOf(
            Size.lengthOfArray(setIntersectionExpr)
        ).divideBy(tagCount.takeIf { it > 0 } ?: 1)

        val tagScore: AggregationExpression = if (tagCount > 0) {
            Multiply.valueOf(tagFractionExpr).multiplyBy(tagWeight)
        } else {
            // constant zero
            Multiply.valueOf(Divide.valueOf(ifNull("rating").then(0)).divideBy(1)).multiplyBy(0.0)
        }

        // timeScore = (1 - min(|time - midTime| / timeRange, 1)) * timeWeight
        // we build: let d = abs(time - midTime) ; clamp = min(d / timeRange, 1) ; timeScore = (1 - clamp) * timeWeight

        val diffExpr = Abs.absoluteValueOf(Subtract.valueOf("time").subtract(midTime))
        val normalizedDiffExpr = Divide.valueOf(diffExpr).divideBy(timeRange)
        val clampedExpr = Min.minOf(normalizedDiffExpr).and(LiteralOperators.valueOf(1.0).asLiteral())
        val timeScore: AggregationExpression = Multiply.valueOf(
            Subtract.valueOf(LiteralOperators.valueOf(1.0).asLiteral()).subtract(clampedExpr)
        ).multiplyBy(timeWeight)

        // userAvgScore = (ifNull(averageUserRating, ifNull(rating,0)) / 5) * userWeight
        val avgOrRatingExpr = ifNull("averageUserRating").then(ifNull("rating").then(0))
        val userAvgScore: AggregationExpression = Multiply.valueOf(
            Divide.valueOf(avgOrRatingExpr).divideBy(5)
        ).multiplyBy(userWeight)

        // Sum all components
        val relevanceExpr = Add.valueOf(nameScore)
            .add(ingredientScore)
            .add(ratingScore)
            .add(tagScore)
            .add(timeScore)
            .add(userAvgScore)

        // add the computed relevance score to the pipeline
        stages.add(
            addFields().addField("relevanceScore").withValue(relevanceExpr).build()
        )


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
        println(pipeline.toString())
        val results = mongoTemplate.aggregate(pipeline, "meals", MealCardDto::class.java).mappedResults

        println("Aggregation pipeline: $pipeline")

        return UnifiedMealSearchResponse(
            results = results
        )
    }
}