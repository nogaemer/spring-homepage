package de.nogaemer.springhomepage.main.meals.models

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.main.images.Image
import de.nogaemer.springhomepage.main.notes.Note
import de.nogaemer.springhomepage.main.ratings.Rating
import de.nogaemer.springhomepage.main.tags.Tag
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference

/**
 * Represents a meal entity stored in the MongoDB "meals" collection.
 *
 * This is the primary domain model for meal data, containing complete recipe information
 * including ingredients, cooking instructions, nutritional information, and user-generated
 * content such as ratings and notes.
 *
 * ## MongoDB Annotations
 * - [@Document]: Maps to the "meals" collection in MongoDB
 * - [@Id]: MongoDB document identifier (ObjectId)
 * - [@DocumentReference]: Establishes lazy-loaded relationships with referenced collections
 *   (Tags, Ratings, Notes) using MongoDB DBRef
 *
 * ## Relationships
 * - **Tags**: Many-to-many relationship via @DocumentReference. Tags are referenced by ID
 *   and must be pre-loaded via MongoDB aggregation lookups for queries
 * - **Ratings**: One-to-many relationship. User ratings are stored separately and referenced
 * - **Notes**: One-to-many relationship. User notes are stored separately and referenced
 * - **Ingredients**: Embedded MealIngredient objects with @DocumentReference to Ingredient
 *   and IngredientUnit entities
 *
 * ## Performance Considerations
 * - The [rating] field stores a calculated average to avoid real-time aggregation overhead
 * - Complex queries should use MongoDB aggregation pipelines to populate @DocumentReference
 *   fields efficiently (see MealService.findById)
 * - Consider indexing on [name], [tags], [time], and [rating] for common query patterns
 *
 * @property name The display name of the meal (required, indexed for text search)
 * @property ingredients List of ingredients with amounts and units (embedded documents)
 * @property instructions Step-by-step cooking instructions as an ordered list
 * @property images Optional list of image metadata including URLs for display and deletion
 * @property difficulty Difficulty level (e.g., "easy", "medium", "hard")
 * @property time Preparation and cooking time in minutes
 * @property portions Number of servings this recipe yields
 * @property calories Estimated calories per serving
 * @property url Original source URL if imported from external recipe site
 * @property tags Categorization tags referenced via @DocumentReference (lazy-loaded)
 * @property rating Calculated average rating from all user ratings (0.0 if no ratings)
 * @property id MongoDB ObjectId, auto-generated when saved
 * @property ratings User ratings for this meal, populated via @DocumentReference
 * @property notes User notes for this meal, populated via @DocumentReference
 *
 * @see MealIngredient
 * @see Tag
 * @see Rating
 * @see Note
 */
@Document(collection = "meals")
@Data
@AllArgsConstructor
@NoArgsConstructor
data class Meal(
    @NotNull
    val name: String,

    val ingredients: List<MealIngredient>,

    val instructions: List<String>,

    val images: List<Image>?,

    @NotNull
    val difficulty: String,

    val time: Long,

    val portions: Int,

    val calories: Int,

    val url: String = "",

    @NotNull
    @DocumentReference
    var tags: MutableList<Tag> = mutableListOf(),

    @JsonSerialize(using = DoubleSerializer::class)
    var rating: Double = 0.0
) {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: ObjectId? = null

    @DocumentReference
    var ratings: List<Rating> = emptyList()

    @DocumentReference
    var notes: List<Note> = emptyList()

    /**
     * Calculates the average rating from all user ratings for this meal.
     *
     * @return The average rating as a Double, or 0.0 if no ratings exist
     */
    fun calculateRating(): Double {
        ratings.map { println(it.rating) }
        if (ratings.isEmpty()) return 0.0
        return ratings.map { it.rating }.average()
    }
}

/**
 * Custom JSON serializer for Double values to format ratings with one decimal place.
 *
 * Ensures consistent rating display format (e.g., 4.5 instead of 4.500000) in JSON responses.
 */
class DoubleSerializer : JsonSerializer<Double>() {
    override fun serialize(value: Double, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeRawValue(String.format("%.1f", value))
    }
}

