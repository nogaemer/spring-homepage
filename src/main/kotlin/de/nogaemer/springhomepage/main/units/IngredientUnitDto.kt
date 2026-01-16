/**
 * Data transfer object for ingredient unit aggregation results.
 *
 * Used internally in MongoDB aggregation pipelines when additional computed fields
 * or transformations are needed. All fields are nullable to accommodate partial
 * projections from aggregation stages.
 */
package de.nogaemer.springhomepage.main.units

import org.bson.types.ObjectId

/**
 * DTO for unit data from aggregation queries.
 *
 * This class is used as a target for MongoDB aggregation results where units
 * might have computed fields or partial data. All fields except [id] are nullable
 * to support flexible aggregation pipelines.
 *
 * ## Usage Context
 * While [IngredientUnit] represents the actual domain model with non-null
 * constraints, this DTO allows MongoDB aggregations to project subsets of fields
 * or add computed fields without constraint violations.
 *
 * ## Conversion
 * Typically converted back to [IngredientUnit] after aggregation:
 * ```kotlin
 * val unit = IngredientUnit(
 *   dto.abbreviation ?: "",
 *   dto.abbreviationPlural ?: "",
 *   // ... etc
 * )
 * unit.id = dto.id
 * ```
 *
 * @property abbreviation Short form singular (nullable for partial projections)
 * @property abbreviationPlural Short form plural (nullable for partial projections)
 * @property fullName Long form singular (nullable for partial projections)
 * @property fullNamePlural Long form plural (nullable for partial projections)
 * @property countable Whether unit represents discrete items (nullable for partial projections)
 * @property category Classification of unit type (nullable for partial projections)
 * @property description Human-readable explanation (nullable for partial projections)
 * @property id MongoDB ObjectId (always present from database)
 */
data class IngredientUnitDto (
    var abbreviation: String? = null,
    var abbreviationPlural: String? = null,
    var fullName: String? = null,
    var fullNamePlural: String? = null,
    var countable: Boolean? = null,
    var category: String? = null,
    var description: String? = null,
    val id: ObjectId
)