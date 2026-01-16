/**
 * Domain model representing measurement units for ingredients.
 *
 * This entity is stored in the MongoDB "units" collection and defines units of measurement
 * used in recipes (e.g., grams, cups, tablespoons). Each unit supports both singular and
 * plural forms in abbreviated and full formats, plus categorization and countability.
 */
package de.nogaemer.springhomepage.main.units

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.bson.types.ObjectId
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * IngredientUnit entity representing a measurement unit.
 *
 * Provides flexible representation of measurement units with support for:
 * - Multiple display formats (abbreviated and full names)
 * - Pluralization for quantities greater than 1
 * - Categorization (weight, volume, count, etc.)
 * - Countability flag for discrete vs. continuous quantities
 *
 * ## MongoDB Annotations
 * - [@Document]: Maps to "units" collection in MongoDB
 * - [@Id]: MongoDB ObjectId, automatically generated when saved
 * - [@JsonSerialize]: Converts ObjectId to string in JSON responses
 * - [@NotNull]: Ensures required fields are not null
 *
 * ## Relationship with Ingredients
 * Ingredients can reference a default unit via [@DocumentReference],
 * creating a referenced relationship rather than embedding.
 *
 * ## Display Examples
 * - "1 tsp" (singular abbreviated)
 * - "2 tsps" (plural abbreviated)
 * - "1 teaspoon" (singular full)
 * - "2 teaspoons" (plural full)
 *
 * @property abbreviation Short form singular (e.g., "g", "tsp", "cup")
 * @property abbreviationPlural Short form plural (e.g., "gs", "tsps", "cups")
 * @property fullName Long form singular (e.g., "gram", "teaspoon", "cup")
 * @property fullNamePlural Long form plural (e.g., "grams", "teaspoons", "cups")
 * @property countable Whether unit represents discrete items (true) or continuous measure (false)
 * @property category Classification of unit type (e.g., "weight", "volume", "count", "length")
 * @property description Human-readable explanation of unit usage and context
 * @property id MongoDB ObjectId, automatically generated
 */
@Document(collection = "units")
data class IngredientUnit(
    @field:NotNull
    val abbreviation: String,

    @field:NotNull
    val abbreviationPlural: String,

    @field:NotNull
    val fullName: String,

    @field:NotNull
    val fullNamePlural: String,

    @field:NotNull
    val countable: Boolean,

    @field:NotNull
    val category: String,

    @field:NotNull
    val description: String
) {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: ObjectId? = null
}