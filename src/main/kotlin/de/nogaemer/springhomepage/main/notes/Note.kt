/**
 * Domain model representing user notes attached to meals.
 *
 * This entity is stored in the MongoDB "notes" collection and allows users to add
 * personal comments, observations, or modifications to meal recipes. Each note is
 * associated with a specific meal and owned by a user, creating a bidirectional
 * relationship where notes are both stored independently and referenced from meals.
 */
package de.nogaemer.springhomepage.main.notes

import de.nogaemer.springhomepage.main.meals.EntityWithMealId
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * Note entity representing user comments on meals.
 *
 * Implements [EntityWithMealId] to enable automatic management by [BaseService],
 * which handles meal relationship updates and cache invalidation.
 *
 * ## MongoDB Annotations
 * - [@Document]: Maps to "notes" collection in MongoDB
 * - [@Id]: MongoDB ObjectId, automatically generated when saved
 * - [@CreatedDate]: Auto-populated on first save (requires auditing enabled)
 * - [@LastModifiedDate]: Auto-updated on every save
 * - [@JsonSerialize]: Converts ObjectIds to strings in JSON responses
 *
 * ## Bidirectional Relationship
 * - Note stores [mealId] reference to parent meal
 * - Meal document contains array of note references
 * - Both sides maintained automatically by [NoteService]
 *
 * @property mealId Reference to the meal this note belongs to
 * @property note The text content of the note
 * @property date Timestamp when the note was created (auto-populated)
 * @property modifiedDate Timestamp when the note was last modified (auto-updated)
 * @property id MongoDB ObjectId, automatically generated
 * @property userId Reference to the user who created this note
 */
@Document(collection = "notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
data class Note(
    @field:JsonSerialize(using = ToStringSerializer::class)
    override var mealId: ObjectId,
    val note: String,

    @CreatedDate
    var date: LocalDateTime? = null,
    @LastModifiedDate
    var modifiedDate: LocalDateTime? = null
): de.nogaemer.springhomepage.main.meals.EntityWithMealId {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    private var id: ObjectId? = null

    @field:JsonSerialize(using = ToStringSerializer::class)
    override var userId: ObjectId? = null
}