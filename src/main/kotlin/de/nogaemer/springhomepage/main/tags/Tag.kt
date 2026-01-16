/**
 * Domain model representing categorization tags for meals.
 *
 * This entity is stored in the MongoDB "tags" collection and provides a flexible
 * labeling system for meals. Tags can represent dietary categories (vegetarian, gluten-free),
 * cuisines (Italian, Mexican), meal types (breakfast, dinner), or any custom classification.
 */
package de.nogaemer.springhomepage.main.tags

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import jakarta.validation.constraints.Pattern
import org.bson.types.ObjectId
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Tag entity representing a meal classification label.
 *
 * Tags are reusable labels that can be associated with multiple meals. Each tag
 * has a name, type category, description, and color for UI representation.
 *
 * ## MongoDB Annotations
 * - [@Document]: Maps to "tags" collection in MongoDB
 * - [@Id]: MongoDB ObjectId, automatically generated when saved
 * - [@JsonSerialize]: Converts ObjectId to string in JSON responses
 *
 * ## Validation
 * - [@Pattern]: Ensures color is a valid 6-digit hex color code with # prefix
 * - [@NotNull]: Ensures required fields are not null
 *
 * ## Relationship with Meals
 * Tags are embedded directly in Meal documents as an array, not as references.
 * This denormalized approach improves query performance for tag filtering.
 *
 * @property name Display name of the tag (e.g., "Vegetarian", "Italian", "Quick Meal")
 * @property type Category grouping for tags (e.g., "diet", "cuisine", "meal-type")
 * @property description Detailed explanation of what this tag represents
 * @property color Hex color code for UI display (e.g., "#FF5733"), validated by pattern
 * @property id MongoDB ObjectId, automatically generated
 */
@Document(collection = "tags")
data class Tag(
    @field:NotNull
    val name: String,

    @field:NotNull
    val type: String,

    @field:NotNull
    val description: String,

    @field:Pattern(regexp = "^#([A-Fa-f0-9]{6})$")
    @field:NotNull
    val color: String
) {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: ObjectId? = null
}