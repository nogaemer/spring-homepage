package de.nogaemer.springhomepage.main.utils

import org.bson.Document
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation

/**
 * Utility object providing MongoDB aggregation pipeline helpers.
 *
 * Contains reusable aggregation stages for complex document transformations,
 * particularly for enriching meal data with related entity details via lookups.
 *
 * ## Purpose
 * Centralizes common aggregation patterns to:
 * - Avoid code duplication across services
 * - Ensure consistent data transformation
 * - Simplify maintenance of complex pipelines
 *
 * ## Main Use Case
 * Transforming raw meal documents into [de.nogaemer.springhomepage.main.meals.dto.MealCardDto]
 * by looking up and embedding ingredient and unit details.
 *
 * ## MongoDB Aggregation Concepts
 * - **$lookup**: Join operation to bring in related documents
 * - **$map**: Transform array elements
 * - **$filter**: Select matching elements from array
 * - **$arrayElemAt**: Extract element at specific index
 * - **$let**: Define variables for complex expressions
 *
 * @see de.nogaemer.springhomepage.main.filters.FilterService
 * @see de.nogaemer.springhomepage.main.meals.dto.MealCardDto
 */
object AggregationUtils {
    /**
     * Creates a MongoDB $let expression to lookup a specific field from joined array.
     *
     * Generates complex expression that:
     * 1. Filters lookup array to find matching element by ID
     * 2. Takes first matching element (should be only one)
     * 3. Extracts specified field from matched element
     *
     * ## Use Case
     * When you have a lookup array (e.g., temp_ingredients) and need to extract
     * a specific field (e.g., "name") from the element matching the current item's ID.
     *
     * ## Generated Expression Structure
     * ```
     * {
     *   $let: {
     *     vars: {
     *       match: { $arrayElemAt: [{ $filter: ... }, 0] }
     *     },
     *     in: "$$match.<targetField>"
     *   }
     * }
     * ```
     *
     * ## MongoDB Variables
     * - $$item: Current element in outer $map
     * - $$el: Current element in $filter
     * - $$match: Filtered result from $let vars
     *
     * ## Example
     * For ingredient name lookup:
     * ```kotlin
     * lookupField("temp_ingredients", "ingredient", "name")
     * // Returns ingredient name from temp_ingredients array
     * // where _id matches $$item.ingredient
     * ```
     *
     * @param lookupArray Name of the temporary array from $lookup stage
     * @param itemIdField Field name in current item containing the ID to match
     * @param targetField Field name to extract from matched element
     * @return MongoDB Document representing the $let expression with BSON structure
     */
    fun lookupField(lookupArray: String, itemIdField: String, targetField: String): Document {
        return Document(
            "\$let", Document()
                .append(
                    "vars", Document(
                        "match", Document(
                            "\$arrayElemAt", listOf(
                                Document(
                                    "\$filter", Document()
                                        .append("input", "\$$lookupArray")
                                        .append("as", "el")
                                        .append("cond", Document("\$eq", listOf("\$\$el._id", "\$\$item.$itemIdField")))
                                ), 0
                            )
                        )
                    )
                )
                .append("in", "\$\$match.$targetField")
        )
    }

    /**
     * Creates a MongoDB $let expression to lookup entire matched object from joined array.
     *
     * Similar to [lookupField] but returns the entire matched document rather than
     * a specific field. Useful when you need multiple fields from the matched element.
     *
     * ## Difference from lookupField
     * - **lookupField**: Returns single field value (e.g., "name")
     * - **lookupObject**: Returns entire matched document (e.g., {_id, name, category})
     *
     * ## Generated Expression Structure
     * ```
     * {
     *   $let: {
     *     vars: {
     *       match: { $arrayElemAt: [{ $filter: ... }, 0] }
     *     },
     *     in: "$$match"  // Returns entire object
     *   }
     * }
     * ```
     *
     * ## Use Case
     * When downstream processing needs multiple fields from the looked-up entity,
     * returning entire object is more efficient than multiple lookupField calls.
     *
     * ## Example
     * ```kotlin
     * lookupObject("temp_ingredients", "ingredient")
     * // Returns entire ingredient document: {_id, name, category, ...}
     * ```
     *
     * @param lookupArray Name of the temporary array from $lookup stage
     * @param itemIdField Field name in current item containing the ID to match
     * @return MongoDB Document representing the $let expression with BSON structure
     */
    fun lookupObject(lookupArray: String, itemIdField: String): Document {
        return Document(
            "\$let", Document()
                .append(
                    "vars", Document(
                        "match", Document(
                            "\$arrayElemAt", listOf(
                                Document(
                                    "\$filter", Document()
                                        .append("input", "\$$lookupArray")
                                        .append("as", "el")
                                        .append("cond", Document("\$eq", listOf("\$\$el._id", "\$\$item.$itemIdField")))
                                ), 0
                            )
                        )
                    )
                )
                .append("in", "\$\$match")
        )
    }

    /**
     * Creates aggregation stages to project meal documents to MealCardDto format.
     *
     * Generates a complete pipeline sequence that:
     * 1. Looks up ingredient details from ingredients collection
     * 2. Looks up unit details from units collection
     * 3. Projects to MealCardDto structure with enriched ingredient data
     *
     * ## Pipeline Stages
     * 1. **$lookup (ingredients)**: Join meal.ingredients[].ingredient with ingredients._id
     * 2. **$lookup (units)**: Join meal.ingredients[].unit with units._id
     * 3. **$project**: Transform to DTO structure with nested lookups
     *
     * ## Projection Fields
     * - **id**: String representation of _id
     * - **name**: Meal name
     * - **rating**: Average rating
     * - **time**: Preparation time
     * - **difficulty**: Difficulty level
     * - **images**: Image URLs array
     * - **instructions**: Cooking instructions
     * - **matchingRatio**: (Optional) Ingredient match percentage
     * - **relevanceScore**: (Optional) Search relevance score
     * - **ingredients**: Enriched array with:
     *   - name: Ingredient name in recipe
     *   - amount: Quantity needed
     *   - ingredientName: Name from ingredients collection
     *   - ingredientCategory: Category from ingredients collection
     *   - unitAbbreviation: Unit abbreviation (e.g., "kg", "tbsp")
     *   - unitFullName: Full unit name (e.g., "kilogram", "tablespoon")
     *
     * ## Configurable Ingredients Field
     * The [ingredientsField] parameter allows projecting from different source fields:
     * - Default "ingredients": Normal meal ingredients
     * - "matchingIngredients": Subset of ingredients that match search criteria
     *
     * ## N+1 Query Prevention
     * By using $lookup and $map in a single aggregation pipeline, this avoids
     * N+1 query problem where each meal would require separate queries for
     * each ingredient and unit.
     *
     * ## Performance
     * - Single database roundtrip for entire query
     * - Server-side join and transformation
     * - Returns only projected fields (smaller payload)
     * - Indexed lookups (_id based)
     *
     * ## Example Usage
     * ```kotlin
     * val stages = mutableListOf<AggregationOperation>()
     * stages.add(match(...))
     * stages.addAll(AggregationUtils.getMealCardProjectionStages())
     * val pipeline = newAggregation(*stages.toTypedArray())
     * mongoTemplate.aggregate(pipeline, "meals", MealCardDto::class.java)
     * ```
     *
     * @param ingredientsField Name of the ingredients field to project (default: "ingredients")
     * @return List of three aggregation stages (two lookups + one projection)
     *
     * @see de.nogaemer.springhomepage.main.meals.dto.MealCardDto
     * @see lookupField
     */
    fun getMealCardProjectionStages(ingredientsField: String = "ingredients"): List<AggregationOperation> {
        val lookupIngredients = Aggregation.lookup("ingredients", "$ingredientsField.ingredient", "_id", "temp_ingredients")
        val lookupUnits = Aggregation.lookup("units", "$ingredientsField.unit", "_id", "temp_units")

        val projectStage = Aggregation.stage(
            Document(
                "\$project", Document()
                    .append("id", Document("\$toString", "\$_id"))
                    .append("name", 1)
                    .append("rating", 1)
                    .append("time", 1)
                    .append("difficulty", 1)
                    .append("images", 1)
                    .append("instructions", 1)
                    .append("matchingRatio", 1)
                    .append("relevanceScore", 1)

                    .append(
                        ingredientsField, Document(
                            "\$map", Document()
                                .append("input", "\$$ingredientsField")
                                .append("as", "item")
                                .append(
                                    "in", Document()
                                        .append("name", "\$\$item.name")
                                        .append("amount", "\$\$item.amount")
                                        .append(
                                            "ingredientName",
                                            lookupField("temp_ingredients", "ingredient", "name")
                                        )
                                        .append(
                                            "ingredientCategory",
                                            lookupField("temp_ingredients", "ingredient", "category")
                                        )
                                        .append(
                                            "unitAbbreviation",
                                            lookupField("temp_units", "unit", "abbreviation"),
                                        )
                                        .append("unitFullName", lookupField("temp_units", "unit", "fullName"))
                                )
                        )
                    )
            )
        )
        return listOf(lookupIngredients, lookupUnits, projectStage)
    }
}
