/**
 * Service layer for ingredient management with advanced search capabilities.
 *
 * Provides CRUD operations and complex MongoDB aggregation-based search with relevance ranking.
 * The search algorithm scores ingredients based on exact matches, prefix matches, and category matches.
 */
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

/**
 * Service for ingredient operations with intelligent search functionality.
 *
 * @property ingredientRepository Repository for basic CRUD operations
 * @property mongoTemplate MongoDB template for advanced aggregation queries
 */
@Service
class IngredientService(
    val ingredientRepository: IngredientRepository,
    val mongoTemplate: MongoTemplate
) {

    /**
     * Saves a single ingredient to the database.
     *
     * @param ingredient The ingredient to save
     * @return The saved ingredient with generated ID
     */
    fun saveIngredient(ingredient: Ingredient): Ingredient {
        return ingredientRepository.save(ingredient)
    }

    /**
     * Saves multiple ingredients in bulk.
     *
     * @param ingredients List of ingredients to save
     * @return List of saved ingredients with generated IDs
     */
    fun saveIngredients(ingredients: List<Ingredient>): List<Ingredient> {
        return ingredientRepository.saveAll(ingredients)
    }

    /**
     * Removes an ingredient from the database.
     *
     * @param ingredient The ingredient to delete
     */
    fun removeIngredient(ingredient: Ingredient) {
        ingredientRepository.delete(ingredient)
    }

    /**
     * Retrieves ingredients with intelligent search and relevance ranking.
     *
     * Uses MongoDB aggregation pipeline to implement a sophisticated scoring algorithm:
     * - Exact match: 4 points
     * - Starts with query: 2 points
     * - Ends with query: 2 points
     * - Contains query in name: 2 points
     * - Contains query in category: 1 point
     *
     * Results are sorted by computed priority (descending) then name (ascending).
     * The aggregation pipeline also performs a lookup to resolve the unit DocumentReference.
     *
     * Performance considerations:
     * - Uses regex with case-insensitive matching
     * - Escaped input prevents regex injection
     * - Single aggregation pipeline minimizes database round-trips
     *
     * @param limit Maximum number of results to return
     * @param offset Page offset for pagination (multiplied by limit for skip)
     * @param query Search query string (empty string returns all results)
     * @return List of ingredients with resolved unit references, sorted by relevance
     */
    fun getIngredients(limit: Int, offset: Int = 0, query: String): MutableList<Ingredient> {
        val pageable: Pageable = PageRequest.of(offset, limit)
        val stages = mutableListOf<AggregationOperation>()

        if (!query.isBlank()) {

            val escapedRegex = Regex.escape(query)

            val exactRegex = "^$escapedRegex$"
            val startsWithRegex = "^$escapedRegex"
            val endsWithRegex = "$escapedRegex$"

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

        val ingredients: MutableList<Ingredient> = results.mappedResults.map { dto ->
            val ingredient = Ingredient(dto.name, dto.category, dto.unit, dto.priority)
            ingredient.id = dto.id
            ingredient
        }.toMutableList()

        return ingredients
    }

    /**
     * Finds an ingredient by ObjectId.
     *
     * @param objectId The MongoDB ObjectId to search for
     * @return The ingredient if found, null otherwise
     */
    fun findById(objectId: ObjectId): Ingredient? {
        return ingredientRepository.findById(objectId).getOrNull(0)
    }

    /**
     * Finds an ingredient by string representation of ObjectId.
     *
     * @param objectId The ObjectId as a string
     * @return The ingredient if found, null otherwise
     */
    fun findById(objectId: String): Ingredient? {
        return ingredientRepository.findById(objectId).getOrNull()
    }
}
