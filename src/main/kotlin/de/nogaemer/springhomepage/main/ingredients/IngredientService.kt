package de.nogaemer.springhomepage.main.ingredients

import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Order
import org.springframework.data.domain.Sort.by
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.`when`
import org.springframework.data.mongodb.core.aggregation.StringOperators
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class IngredientService(
    val ingredientRepository: IngredientRepository,
    val mongoTemplate: MongoTemplate
) {

    fun saveIngredient(ingredient: Ingredient): Ingredient {
        return ingredientRepository.save(ingredient)
    }

    fun saveIngredients(ingredients: List<Ingredient>): List<Ingredient> {
        return ingredientRepository.saveAll(ingredients)
    }

    fun removeIngredient(ingredient: Ingredient) {
        ingredientRepository.delete(ingredient)
    }

    fun getIngredients(limit: Int, offset: Int = 0, query: String): MutableList<Ingredient> {
        val pageable: Pageable = PageRequest.of(offset, limit)
        val stages = mutableListOf<AggregationOperation>()

        if (!query.isBlank()) {

            val escapedRegex = Regex.escape(query)

            val exactRegex = "^$escapedRegex$"
            val startsWithRegex = "^$escapedRegex"
            val endsWithRegex = "$escapedRegex$"

            // use StringOperators.regexMatch for all string regex checks
            val startsWithExpr = StringOperators.valueOf("name").regexMatch(startsWithRegex, "i")
            val endsWithExpr = StringOperators.valueOf("name").regexMatch(endsWithRegex, "i")

            val explicitNameMatchScore = `when`(StringOperators.valueOf("name").regexMatch(exactRegex, "i"))
                .then(4)
                .otherwise(
                    `when`(startsWithExpr)
                        .then(2)
                        .otherwise(
                            `when`(endsWithExpr)
                                .then(2)
                                .otherwise(0)
                        )
                )

            val nameMatchScore = `when`(StringOperators.valueOf("name").regexMatch(escapedRegex, "i"))
                .then(2)
                .otherwise(0)

            val categoryMatchScore = `when`(StringOperators.valueOf("category").regexMatch(escapedRegex, "i"))
                .then(1)
                .otherwise(0)

            val priorityExpr = ArithmeticOperators.Add
                .valueOf(explicitNameMatchScore)
                .add(nameMatchScore)
                .add(categoryMatchScore)


            stages.addAll(
                listOf(
                    match(
                        Criteria().orOperator(
                            Criteria.where("name").regex(escapedRegex, "i"),
                            Criteria.where("category").regex(escapedRegex, "i")
                        )
                    ),
                    addFields().addField("priority").withValue(priorityExpr).build(),
                    sort(by(Order.desc("priority"), Order.asc("name"))),
                )
            )
        }

        stages.addAll(
            listOf(
                skip((offset * limit).toLong()),
                limit(limit.toLong()),
                lookup("units", "unit", "_id", "unit"),
                unwind("unit", true)
            )
        )
        val pipeline = newAggregation(*stages.toTypedArray())
        val results = mongoTemplate.aggregate(pipeline, "ingredients", IngredientDto::class.java)

        // Map IngredientDto returned by the aggregation into Ingredient domain objects
        val ingredients: MutableList<Ingredient> = results.mappedResults.map { dto ->
            val ingredient = Ingredient(dto.name, dto.category, dto.unit, dto.priority)
            ingredient.id = dto.id
            ingredient
        }.toMutableList()

        return ingredients
    }

    fun findById(objectId: ObjectId): Ingredient? {
        return ingredientRepository.findById(objectId).getOrNull(0)
    }

    fun findById(objectId: String): Ingredient? {
        return ingredientRepository.findById(objectId).getOrNull()
    }
}
