/**
 * Service providing real-time search completion suggestions for meal names.
 *
 * Uses MongoDB aggregation to implement token-based relevance scoring for meal search.
 * Results are sorted by relevance score and rating to surface the most relevant meals first.
 */
package de.nogaemer.springhomepage.main.search

import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.`when`
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import java.util.regex.Pattern

/**
 * Search completion result with relevance scoring.
 *
 * @property text The completion text (meal name)
 * @property relevanceScore Computed relevance score from aggregation
 * @property type The type of result ("meal" or "ingredient")
 */
data class SearchCompletion(
    val text: String,
    val relevanceScore: Int? = null,
    val type: String
)

/**
 * Service for generating autocomplete suggestions during search.
 *
 * @property mongoTemplate MongoDB template for aggregation queries
 */
@Service
class SearchCompletionService(
    private val mongoTemplate: MongoTemplate
) {

    /**
     * Generates meal name completions based on tokenized query matching.
     *
     * Algorithm:
     * 1. Tokenize query by whitespace
     * 2. Match meals where name contains ANY token (OR logic)
     * 3. Score each meal by counting matching tokens
     * 4. Sort by relevance score (desc) then rating (desc)
     * 5. Return top 10 results
     *
     * MongoDB aggregation pipeline:
     * - $match: Pre-filter meals containing at least one token
     * - $addFields: Calculate relevance score (sum of token matches)
     * - $sort: Order by score and rating
     * - $limit: Cap at 10 results
     * - $project: Return only name and score
     *
     * Performance: Uses regex indexes on meal.name field for efficient matching.
     *
     * @param query The partial search query
     * @return List of SearchCompletion objects ordered by relevance
     */
    fun getCompletions(query: String): List<SearchCompletion> {
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<SearchCompletion>()
        val stages = mutableListOf<AggregationOperation>()


        val tokens = query
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (tokens.isNotEmpty()) {
            val orCriteria = Criteria().orOperator(
                *tokens.map { token ->
                    Criteria.where("name").regex(Pattern.quote(token), "i")
                }.toTypedArray()
            )

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

            stages += Aggregation.match(orCriteria)
            stages += Aggregation.addFields().addField("relevanceScore").withValue(scoreExpression!!).build()
            stages += Aggregation.sort(Sort.by(Sort.Order.desc("relevanceScore"), Sort.Order.desc("rating")))
            stages += Aggregation.limit(10)
            stages += Aggregation.project("name", "relevanceScore").andExclude("_id")
        }

        val pipeline = Aggregation.newAggregation(*stages.toTypedArray())
        val meals = mongoTemplate.aggregate(pipeline, "meals", Map::class.java).mappedResults


        results.addAll(meals.map { SearchCompletion(it["name"].toString(), it["relevanceScore"] as Int?, "meal") })



        return results
    }
}
