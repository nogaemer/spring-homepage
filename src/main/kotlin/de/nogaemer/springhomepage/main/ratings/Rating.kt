/**
 * Domain model representing a user's rating for a meal.
 *
 * Ratings are stored in the MongoDB "ratings" collection and track numerical scores
 * (typically 1-5 stars) that users assign to meals. Ratings are used to calculate
 * average meal scores and filter favorite meals.
 */
package de.nogaemer.springhomepage.main.ratings

import de.nogaemer.springhomepage.main.meals.EntityWithMealId
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * Rating entity linking a user, meal, and numerical score.
 *
 * Implements EntityWithMealId to support generic operations on meal-related entities.
 * When ratings are created/updated/deleted, the service automatically recalculates
 * the meal's average rating field.
 *
 * @property mealId ObjectId of the rated meal
 * @property rating Numerical rating value (typically 1-5)
 * @property date Timestamp when rating was created (auto-populated)
 * @property modifiedDate Timestamp of last modification (auto-updated)
 * @property id MongoDB ObjectId, automatically generated
 * @property userId ObjectId of the user who created the rating
 */
@Document(collection = "ratings")
@Data
@NoArgsConstructor @AllArgsConstructor
data class Rating(
    @field:JsonSerialize(using = ToStringSerializer::class)
    override var mealId: ObjectId,

    @NotNull
    var rating: Int,

    @CreatedDate
    var date: LocalDateTime? = null,

    @LastModifiedDate
    var modifiedDate: LocalDateTime? = null
): EntityWithMealId {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    private var id: ObjectId? = null

    @field:JsonSerialize(using = ToStringSerializer::class)
    override var userId: ObjectId? = null
}