/**
 * Service layer for note management with meal relationship handling.
 *
 * Extends [BaseService] to provide CRUD operations for notes with automatic
 * meal relationship management and cache synchronization. Notes allow users
 * to add personal comments or modifications to meal recipes.
 */
package de.nogaemer.springhomepage.main.notes

import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.main.meals.MealRepository
import de.nogaemer.springhomepage.main.meals.models.Meal
import de.nogaemer.springhomepage.user.UserRepository
import de.nogaemer.springhomepage.user.UserResponse
import de.nogaemer.springhomepage.user.UserService
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service

/**
 * Service for note operations with meal relationship synchronization.
 *
 * Inherits from [BaseService] to automatically manage:
 * - Bidirectional meal-note relationships
 * - Cache invalidation for meal updates
 * - User authorization for delete operations
 * - Single note per user per meal constraint
 *
 * @property repository Repository for note CRUD operations
 * @property userRepository Repository for resolving user information in responses
 */
@Service
class NoteService(
    val repository: NoteRepository,
    mealRepository: MealRepository,
    val userRepository: UserRepository,
    mongoTemplate: MongoTemplate,
    userService: UserService,

    @Autowired
    cacheManager: CacheManager
) : de.nogaemer.springhomepage.main.meals.BaseService<Note, ObjectId>(
    repository,
    mealRepository,
    mongoTemplate,
    userService,
    cacheManager
) {

    /**
     * Finds a note by user ID and meal ID.
     *
     * Required implementation for [BaseService] to enforce single note per user per meal.
     *
     * @param userId The user's ObjectId
     * @param mealId The meal's ObjectId
     * @return The note if found, null otherwise
     */
    override fun findByUserId(userId: ObjectId, mealId: ObjectId): Note? {
        return repository.findByUserIdAndMealId(userId, mealId)
    }

    /**
     * MongoDB field name in Meal document for note references.
     *
     * Used by [BaseService] for $push/$pull operations on meal.notes array.
     */
    override val entityFieldName = "notes"

    /**
     * Retrieves all notes from the database.
     *
     * @return List of all notes (use with caution on large datasets)
     */
    fun findAll(): List<Note> {
        return repository.findAll()
    }

    /**
     * Retrieves all notes for a specific meal with user information.
     *
     * Enriches each note with the author's username by joining with user data.
     * Useful for displaying notes with author attribution in the UI.
     *
     * @param mealId The meal's ObjectId
     * @return List of notes wrapped in [NoteResponse] with user details
     * @throws IdNotFoundException If a note references a non-existent user
     */
    fun getNotesByMealId(mealId: ObjectId): List<NoteResponse> {
        val ratings = mutableListOf<NoteResponse>()

        repository.findByMealId(mealId).forEach {
            val user = userRepository.findById(it.userId) ?: throw IdNotFoundException("User not found")

            ratings.add(
                NoteResponse(
                    it,
                    UserResponse(
                        user.id!!,
                        user.login
                    )
                )
            )
        }
        return ratings
    }

    /**
     * Creates a new note and associates it with its meal.
     *
     * Delegates to [BaseService.create] which:
     * 1. Validates meal exists
     * 2. Assigns current user if not specified
     * 3. Enforces single note per user per meal
     * 4. Pushes note reference to meal.notes array
     * 5. Invalidates meal caches
     *
     * @param response The note to create
     * @return The saved note with generated ID
     * @throws IdNotFoundException If meal doesn't exist
     * @throws de.nogaemer.springhomepage.exceptions.AlreadyReported If user already has a note for this meal
     */
    override fun create(response: Note): Note {
        return super.create(response)
    }

    /**
     * Deletes a note by ID with authorization check.
     *
     * Retrieves the note and delegates to [delete] with entity.
     *
     * @param id The note's ObjectId
     * @return The deleted note
     * @throws IdNotFoundException If note doesn't exist
     * @throws RuntimeException If user is not authorized to delete
     */
    fun delete(id: ObjectId): Note {
        val note = repository.findById(id)
            .orElseThrow { IdNotFoundException("Note not found") }

        return delete(id, note)
    }

    /**
     * Deletes a note and removes its reference from the meal.
     *
     * Delegates to [BaseService.delete] which:
     * 1. Verifies user is owner or ADMIN
     * 2. Pulls note reference from meal.notes array
     * 3. Deletes note document
     * 4. Invalidates meal caches
     *
     * @param id The note's ObjectId
     * @param entity The note to delete
     * @return The deleted note
     * @throws RuntimeException If user is not authorized to delete
     */
    override fun delete(id: ObjectId, entity: Note): Note {
        return super.delete(id, entity)
    }

    /**
     * Deletes all notes associated with a meal.
     *
     * Used during meal deletion cascade to clean up orphaned notes.
     * Does NOT update meal document since meal is being deleted.
     *
     * @param meal The meal whose notes should be deleted
     */
    fun deleteNotesByMeal(meal: Meal) {
        repository.deleteAllByMealId(meal.id!!)
    }

    /**
     * Finds all notes associated with a specific meal.
     *
     * Returns raw [Note] entities without user enrichment.
     * Use [getNotesByMealId] for user-enriched responses.
     *
     * @param objectId The meal's ObjectId
     * @return List of notes for the meal
     */
    fun findByMealId(objectId: ObjectId): List<Note> {
        return repository.findByMealId(objectId)
    }

}
