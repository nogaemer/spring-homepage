/**
 * Service layer for ingredient unit management with advanced search capabilities.
 *
 * Provides CRUD operations for measurement units and implements MongoDB aggregation-based
 * search with relevance ranking. Search scores matches across abbreviated forms, full names,
 * descriptions, and categories.
 */
package de.nogaemer.springhomepage.main.units

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

/**
 * Service for unit operations with intelligent search functionality.
 *
 * Implements sophisticated search algorithm that ranks units by relevance
 * across multiple fields, prioritizing abbreviation and name matches over
 * description and category matches.
 *
 * @property unitRepository Repository for basic CRUD operations
 * @property mongoTemplate MongoDB template for advanced aggregation queries
 */
@Service
class UnitService(
    val unitRepository: UnitRepository,
    val mongoTemplate: MongoTemplate
) {

    /**
     * Saves a single unit to the database.
     *
     * @param unit The unit to save
     * @return The saved unit with generated ID if new
     */
    fun saveUnit(unit: IngredientUnit): IngredientUnit {
        return unitRepository.save(unit)
    }

    /**
     * Saves multiple units in bulk.
     *
     * @param units List of units to save
     * @return List of saved units with generated IDs
     */
    fun saveUnits(units: List<IngredientUnit>): List<IngredientUnit> {
        return unitRepository.saveAll(units)
    }

    /**
     * Removes a unit from the database.
     *
     * Warning: Deleting a unit that is referenced by ingredients will break those references.
     * Consider checking for usage before deletion.
     *
     * @param unit The unit to delete
     */
    fun removeUnit(unit: IngredientUnit) {
        unitRepository.delete(unit)
    }

    /**
     * Retrieves units with intelligent search and relevance ranking.
     *
     * Uses MongoDB aggregation pipeline to implement a sophisticated scoring algorithm:
     * - Abbreviation match (singular or plural): 4 points
     * - Full name match (singular or plural): 4 points
     * - Description match: 2 points
     * - Category match: 1 point
     *
     * Results are sorted by computed priority (descending) then full name (ascending).
     * Empty query returns all units sorted by full name with pagination.
     *
     * ## Search Algorithm
     * The aggregation pipeline:
     * 1. Pre-filters documents matching query in any field (case-insensitive)
     * 2. Computes priority score using $regexMatch conditionals
     * 3. Sorts by priority descending, then fullName ascending
     * 4. Applies pagination with skip and limit
     *
     * ## MongoDB Conditional Scoring
     * Each field group uses $or logic to check both singular and plural forms:
     * ```javascript
     * {
     *   $or: [
     *     { $regexMatch: { input: "$abbreviation", regex: query, options: "i" } },
     *     { $regexMatch: { input: "$abbreviationPlural", regex: query, options: "i" } }
     *   ]
     * }
     * ```
     *
     * ## Performance Considerations
     * - Pre-filter reduces scoring computation to relevant candidates only
     * - Uses regex with case-insensitive matching
     * - Single aggregation pipeline minimizes database round-trips
     *
     * @param limit Maximum number of results to return
     * @param offset Page offset for pagination (multiplied by limit for skip)
     * @param query Search query string (empty string returns all results)
     * @return List of units sorted by relevance
     */
    fun getUnits(limit: Int, offset: Int = 0, query: String): MutableList<IngredientUnit> {
        val pageable: Pageable = PageRequest.of(offset, limit)

        if (query.isBlank()) {
            return unitRepository.findAll(pageable).content
        }

        val regex = ".*${query}.*"

        val abbreviationMatchScore = `when`({ _ ->
            Document(
                "\$or", listOf(
                    Document(
                        "\$regexMatch",
                        Document("input", "\$abbreviation")
                            .append("regex", regex)
                            .append("options", "i")
                    ),
                    Document(
                        "\$regexMatch",
                        Document("input", "\$abbreviationPlural")
                            .append("regex", regex)
                            .append("options", "i")
                    )
                )
            )
        }).then(4).otherwise(0)

        val nameMatchScore = `when`({ _ ->
            Document(
                "\$or", listOf(
                    Document(
                        "\$regexMatch",
                        Document("input", "\$fullName")
                            .append("regex", regex)
                            .append("options", "i")
                    ),
                    Document(
                        "\$regexMatch",
                        Document("input", "\$fullNamePlural")
                            .append("regex", regex)
                            .append("options", "i")
                    )
                )
            )
        }).then(4).otherwise(0)

        val descriptionMatchScore = `when`({ _ ->
            Document(
                "\$regexMatch",
                Document("input", "\$description")
                    .append("regex", regex)
                    .append("options", "i")
            )
        }).then(2).otherwise(0)

        val categoryMatchScore = `when`({ _ ->
            Document(
                "\$regexMatch",
                Document("input", "\$category")
                    .append("regex", regex)
                    .append("options", "i")
            )
        }).then(1).otherwise(0)

        val priorityExpr = ArithmeticOperators.Add.valueOf(abbreviationMatchScore)
            .add(nameMatchScore)
            .add(descriptionMatchScore)
            .add(categoryMatchScore)

        val aggregation = newAggregation(
            match(
                Criteria().orOperator(
                    Criteria.where("abbreviation").regex(regex, "i"),
                    Criteria.where("abbreviationPlural").regex(regex, "i"),
                    Criteria.where("fullName").regex(regex, "i"),
                    Criteria.where("fullNamePlural").regex(regex, "i"),
                    Criteria.where("description").regex(regex, "i"),
                    Criteria.where("category").regex(regex, "i")
                )
            ),
            addFields().addField("priority").withValue(priorityExpr).build(),
            sort(by(Order.desc("priority"), Order.asc("fullName"))),
            skip((offset * limit).toLong()),
            limit(limit.toLong())
        )

        val results = mongoTemplate.aggregate(aggregation, "units", IngredientUnit::class.java)
        return results.mappedResults.toMutableList()
    }

    /**
     * Finds a unit by ObjectId.
     *
     * @param objectId The MongoDB ObjectId to search for
     * @return The unit if found, null otherwise
     */
    fun findById(objectId: ObjectId): IngredientUnit? {
        return unitRepository.findById(objectId).getOrNull(0)
    }

    /**
     * Finds a unit by string representation of ObjectId.
     *
     * @param objectId The ObjectId as a string
     * @return The unit if found, null otherwise
     */
    fun findById(objectId: String): IngredientUnit? {
        return unitRepository.findById(objectId).getOrNull()
    }
}