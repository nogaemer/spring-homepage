package de.nogaemer.springhomepage.main.search

import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.`when`
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import java.util.regex.Pattern

data class SearchCompletion(
    val text: String,
    val relevanceScore: Int? = null,
    val type: String // "meal" or "ingredient"
)

@Service
class SearchCompletionService(
    private val mongoTemplate: MongoTemplate
) {

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
