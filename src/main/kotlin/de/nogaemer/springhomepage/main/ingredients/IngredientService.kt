package de.nogaemer.springhomepage.main.ingredients

import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Order
import org.springframework.data.domain.Sort.by
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.`when`
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

        if (query.isBlank()) {
            return ingredientRepository.findAll(pageable).content
        }

        val regex = ".*${query}.*"

        val nameMatchScore = `when`({ _ ->
            Document(
                "\$regexMatch",
                Document("input", "\$name")
                    .append("regex", regex)
                    .append("options", "i")
            )
        }).then(4).otherwise(0)

        val categoryMatchScore = `when`({ _ ->
            Document(
                "\$regexMatch",
                Document("input", "\$category")
                    .append("regex", regex)
                    .append("options", "i")
            )
        }).then(2).otherwise(0)

        val priorityExpr = ArithmeticOperators.Add.valueOf(nameMatchScore).add(categoryMatchScore)

        val aggregation = newAggregation(
            match(
                Criteria().orOperator(
                    Criteria.where("name").regex(regex, "i"),
                    Criteria.where("category").regex(regex, "i")
                )
            ),
            addFields().addField("priority").withValue(priorityExpr).build(),
            sort(by(Order.desc("priority"), Order.asc("name"))),
            skip((offset * limit).toLong()),
            limit(limit.toLong())
        )

        val results = mongoTemplate.aggregate(aggregation, "ingredients", Ingredient::class.java)
        return results.mappedResults.toMutableList()
    }

    fun findById(objectId: ObjectId): Ingredient? {
        return ingredientRepository.findById(objectId).getOrNull(0)
    }

    fun findById(objectId: String): Ingredient? {
        return ingredientRepository.findById(objectId).getOrNull()
    }
}
