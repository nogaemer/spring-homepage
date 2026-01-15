/**
 * Data Transfer Object for ingredient aggregation queries.
 *
 * This DTO is used as the result type for MongoDB aggregation pipelines that join
 * ingredient data with unit information, particularly in search and filter operations.
 */
package de.nogaemer.springhomepage.main.ingredients

import de.nogaemer.springhomepage.main.units.IngredientUnit
import org.bson.types.ObjectId

/**
 * DTO containing ingredient data with resolved unit reference.
 *
 * @property id MongoDB ObjectId of the ingredient
 * @property name The display name of the ingredient
 * @property category The category classification
 * @property unit The fully resolved IngredientUnit object (from DocumentReference lookup)
 * @property priority Computed search relevance score from aggregation pipeline
 */
data class IngredientDto(
    var id: ObjectId,
    val name: String,
    val category: String,
    val unit: IngredientUnit? = null,
    val priority: Int? = null
)

