/**
 * Spring Data MongoDB repository for Rating entities.
 *
 * Provides CRUD operations and custom queries for rating data access.
 */
package de.nogaemer.springhomepage.main.ratings

import de.nogaemer.springhomepage.main.notes.Note
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository interface for rating database operations.
 */
@Repository
interface RatingRepository: MongoRepository<Rating, ObjectId> {

    /**
     * Finds a rating by user and meal IDs (for checking existing ratings).
     *
     * @param userId The user's ObjectId
     * @param mealId The meal's ObjectId
     * @return The rating if found, null otherwise
     */
    fun findByUserIdAndMealId(userId: ObjectId, mealId: ObjectId): Rating?

    /**
     * Deletes all ratings for a specific meal (used when meal is deleted).
     *
     * @param mealId The meal's ObjectId
     */
    fun deleteAllByMealId(mealId: ObjectId)

    /**
     * Finds all ratings for a specific meal.
     *
     * @param mealId The meal's ObjectId
     * @return List of all ratings for the meal
     */
    fun findByMealId(mealId: ObjectId):List<Rating>
}
