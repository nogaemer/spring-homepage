package de.nogaemer.springhomepage.main.meals

import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.ArrayOperators
import org.springframework.data.mongodb.core.aggregation.EvaluationOperators
import org.springframework.data.mongodb.core.query.Criteria

/**
 * Small isolated test helper so you can iterate on the rating-lookup building logic
 * without touching the main service. Copy / edit this file to try variations.
 */
class UnifiedMealSearchLookupTest {

    /**
     * Build aggregation stages for rating lookup like the service does.
     * This is intentionally local and modifiable for experimentation in tests.
     */
    private fun buildRatingLookupStages(
        userIdStrings: List<String>?,
        minUserRating: Double?,
        requireUserRatingMatch: Boolean
    ): List<AggregationOperation> {
        val stages = mutableListOf<AggregationOperation>()

        val userIds = userIdStrings
            ?.mapNotNull { runCatching { ObjectId(it) }.getOrNull() }
            ?: emptyList()

        println(userIds)

        val needsRatingLookup = userIds.isNotEmpty() || minUserRating != null || requireUserRatingMatch

        if (needsRatingLookup) {
            stages += Aggregation.lookup("ratings", "_id", "mealId", "userRatings")

            // Filter userRatings by userIds (if provided)
            if (userIds.isNotEmpty()) {
                // Build an $expr : { $in: ["$$rating.userId", userIds] }
                val expr = ArrayOperators.In.arrayOf(userIds).containsValue("\$userId")
                val matchOp = Aggregation.match(EvaluationOperators.Expr.valueOf(expr))

                stages += Aggregation.lookup()
                    .from("ratings")
                    .localField("_id")
                    .foreignField("mealId")
                    .pipeline(matchOp)        // only ratings whose userId is in userIds
                    .`as`("userRatings")
            }

            // Filter userRatings by minUserRating (if provided)
            val minUserRating = minUserRating
            if (minUserRating != null) {
                val filterByMin = Document(
                    "\$filter", Document()
                        .append("input", "\$ratings")
                        .append("as", "r")
                        .append("cond", Document("\$gte", listOf("\$\$r.rating", minUserRating)))
                )
                stages += Aggregation.addFields().addFieldWithValue("userRatings", filterByMin).build()
            }

            // averageUserRating = (size==0 ? 0 : avg(userRatings.rating))
            val avgExpr = Document(
                "\$cond", Document()
                    .append("if", Document("\$eq", listOf(Document("\$size", "\$userRatings"), 0)))
                    .append("then", 0)
                    .append("else", Document("\$avg", "\$userRatings.rating"))
            )
            stages += Aggregation.addFields().addFieldWithValue("averageUserRating", avgExpr).build()

            if (requireUserRatingMatch) {
                stages += Aggregation.match(Criteria.where("userRatings").not().size(0))
            }
        }

        return stages
    }

    @Test
    fun `build rating lookup with userIds does not throw`() {
        val userIds = listOf("669e8cc12c91a20e9de8bdee")

        assertDoesNotThrow {
            val stages = buildRatingLookupStages(userIds, null, false)
            println("Built ${stages.size} stages for userIds lookup:")
            stages.forEachIndexed { i, s -> println("[$i] $s") }

            // build Aggregation pipeline from stages so you can inspect / run it
            val pipeline = Aggregation.newAggregation(*stages.toTypedArray())
            println("Aggregation pipeline: $pipeline")

            // If you have a configured MongoTemplate in the test context you can run it like this:
            // val results = mongoTemplate.aggregate(pipeline, "meals", Document::class.java).mappedResults
            // println("Results size: ${'$'}{results.size}")
        }
    }

    @Test
    fun `build rating lookup with minUserRating and requireMatch does not throw`() {
        assertDoesNotThrow {
            val stages = buildRatingLookupStages(null, 4.0, true)
            println("Built ${stages.size} stages for minUserRating lookup:")
            stages.forEachIndexed { i, s -> println("[$i] $s") }

            // build Aggregation pipeline from stages so you can inspect / run it
            val pipeline = Aggregation.newAggregation(*stages.toTypedArray())
            println("Aggregation pipeline: $pipeline")

            // If you have a configured MongoTemplate in the test context you can run it like this:
            // val results = mongoTemplate.aggregate(pipeline, "meals", Document::class.java).mappedResults
            // println("Results size: ${'$'}{results.size}")
        }
    }
}
