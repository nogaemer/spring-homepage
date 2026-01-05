package de.nogaemer.springhomepage.main.filters

import de.nogaemer.springhomepage.main.meals.dto.MealCardDto
import de.nogaemer.springhomepage.main.tags.TagRepository
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

@Service
class FilterService(
    val userRepository: UserRepository,
    val tagRepository: TagRepository,
    val userService: UserService,
    private val mongoTemplate: MongoTemplate
) {

    fun getFilters(): FilterResponse {
        val users = userRepository.findAll().map { UserResponse(it.id!!, it.name) }
        val tags = tagRepository.findAll()

        return FilterResponse(users, tags)
    }


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
