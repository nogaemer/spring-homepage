/**
 * MongoDB repository interface for note persistence operations.
 *
 * Provides CRUD operations and custom queries for notes.
 * Spring Data MongoDB automatically implements basic operations and
 * derives query implementations from method names.
 */
package de.nogaemer.springhomepage.main.notes

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

/**
 * Repository for note database operations.
 *
 * Extends [MongoRepository] to inherit standard CRUD methods (save, findById, delete, etc.).
 * Custom query methods are derived from method names using Spring Data naming conventions.
 */
@Repository
interface NoteRepository: MongoRepository<Note, ObjectId> {

    /**
     * Finds a note by user ID and meal ID.
     *
     * Used to enforce single note per user per meal constraint.
     * Query: `{ "userId": userId, "mealId": mealId }`
     *
     * @param userId The user's ObjectId
     * @param mealId The meal's ObjectId
     * @return The matching note, or null if not found
     */
    fun findByUserIdAndMealId(userId: ObjectId, mealId: ObjectId): Note?

    /**
     * Deletes all notes associated with a specific meal.
     *
     * Used during meal deletion cascade to clean up orphaned notes.
     * Query: `{ "mealId": mealId }`
     *
     * @param mealId The meal's ObjectId
     */
    fun deleteAllByMealId(mealId: ObjectId)

    /**
     * Finds all notes for a specific meal.
     *
     * Returns all notes associated with a meal, typically for display purposes.
     * Query: `{ "mealId": mealId }`
     *
     * @param mealId The meal's ObjectId
     * @return List of notes for the meal (empty list if none found)
     */
    fun findByMealId(mealId: ObjectId): List<Note>
}
