/**
 * Domain model representing an ingredient in the meal planning system.
 *
 * This entity is stored in the MongoDB "ingredients" collection and represents individual
 * food items that can be used in meal recipes. Each ingredient has a name, category,
 * optional measurement unit, and priority for search ranking.
 */
package de.nogaemer.springhomepage.main.ingredients

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.main.units.IngredientUnit
import org.bson.types.ObjectId
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference

/**
 * Ingredient entity representing a food item in the database.
 *
 * @property name The display name of the ingredient (e.g., "Tomato", "Olive Oil")
 * @property category The category this ingredient belongs to (e.g., "Vegetables", "Dairy", "Spices")
 * @property unit Optional reference to the default measurement unit for this ingredient
 * @property priority Optional search priority for relevance ranking in queries
 * @property id MongoDB ObjectId, automatically generated when saved
 */
@Document(collection = "ingredients")
data class Ingredient(
    @field:NotNull
    val name: String,

    @field:NotNull
    val category: String,

    @DocumentReference
    val unit: IngredientUnit? = null,

    val priority: Int? = null
) {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: ObjectId? = null
}

