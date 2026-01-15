package de.nogaemer.springhomepage.main.meals

import de.nogaemer.springhomepage.exceptions.AlreadyReported
import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.main.meals.models.Meal
import de.nogaemer.springhomepage.user.Role
import de.nogaemer.springhomepage.user.UserService
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.repository.MongoRepository

/**
 * Abstract base service for managing entities that are associated with meals.
 *
 * This class provides common CRUD operations for entities like Ratings, Notes, or other
 * meal-related content that:
 * 1. Are stored in their own MongoDB collections
 * 2. Reference a parent Meal via [mealId]
 * 3. Are owned by a specific user via [userId]
 * 4. Are also referenced from the Meal document (bidirectional relationship)
 *
 * ## Relationship Management
 * When entities are created or deleted, this service automatically:
 * - Updates the corresponding array in the parent Meal document (e.g., meal.ratings)
 * - Manages cache invalidation for affected meals
 * - Enforces single-entity-per-user-per-meal constraint
 *
 * ## Cache Management
 * Operations invalidate caches to maintain consistency:
 * - **create/delete**: Clears specific meal from "meals" cache and all "allMeals" entries
 * - Cache clearing ensures subsequent queries reflect the updated relationships
 *
 * ## User Authorization
 * - Creation: Auto-assigns current user if [userId] is null
 * - Deletion: Only owner or ADMIN role can delete entities
 * - Prevents duplicate submissions per user per meal
 *
 * ## Implementing Classes
 * Subclasses must implement:
 * - [findByUserId]: Query logic to find user's entity for a specific meal
 * - [entityFieldName]: MongoDB field name in Meal document (e.g., "ratings", "notes")
 *
 * @param T Entity type that implements [EntityWithMealId]
 * @param ID Entity ID type (typically ObjectId)
 * @param repository MongoDB repository for entity persistence
 * @param mealRepository Repository for parent meal lookups and validation
 * @param mongoTemplate MongoTemplate for direct document updates (push/pull operations)
 * @param userService Service for current user context and authorization
 * @param cacheManager Spring cache manager for meal cache invalidation
 *
 * @see EntityWithMealId
 * @see de.nogaemer.springhomepage.main.ratings.RatingService
 * @see de.nogaemer.springhomepage.main.notes.NoteService
 */
abstract class BaseService<T : EntityWithMealId, ID : Any>(
    private val repository: MongoRepository<T, ID>,
    private val mealRepository: MealRepository,
    private val mongoTemplate: MongoTemplate,
    private val userService: UserService,

    @Autowired private val cacheManager: CacheManager
) {

    /**
     * Creates a new entity and associates it with its parent meal.
     *
     * ## Process
     * 1. Validates that the referenced meal exists
     * 2. Auto-assigns current user if not specified
     * 3. Checks for duplicate submission (one entity per user per meal)
     * 4. Saves entity to its collection
     * 5. Pushes entity reference to parent meal's array field
     * 6. Invalidates relevant caches
     *
     * ## MongoDB Operations
     * - INSERT: Saves entity to its dedicated collection
     * - UPDATE: Uses $push to add entity reference to meal document
     *
     * ## Cache Impact
     * - Invalidates specific meal in "meals" cache
     * - Clears entire "allMeals" cache
     *
     * @param response The entity to create
     * @return The saved entity with generated ID
     * @throws IdNotFoundException If referenced meal doesn't exist
     * @throws AlreadyReported If user already has an entity for this meal
     */
    open fun create(response: T): T {
        val meal = mealRepository.findById(response.mealId).orElseThrow { throw IdNotFoundException("Meal not found") }

        if (response.userId == null) response.userId = userService.getCurrentUser().id

        findByUserId(response.userId!!, meal.id!!)?.let {
            if (it.mealId == response.mealId) throw AlreadyReported(
                "User already submitted this type of entity for this meal",
                it
            )
        }

        val savedEntity = repository.save(response)

        mongoTemplate.update(Meal::class.java).matching(Criteria.where("id").`is`(savedEntity.mealId))
            .apply(Update().push(entityFieldName, savedEntity)).first()

        cacheManager.getCache("meals")!!.put(savedEntity.mealId, meal)
        cacheManager.getCache("allMeals")!!.clear()

        return savedEntity
    }

    /**
     * Deletes an entity and removes its reference from the parent meal.
     *
     * ## Authorization
     * Only the entity owner or users with ADMIN role can delete.
     * Throws RuntimeException for unauthorized attempts.
     *
     * ## Process
     * 1. Validates user has permission to delete
     * 2. Pulls entity reference from parent meal's array field
     * 3. Deletes entity from its collection
     * 4. Invalidates relevant caches
     *
     * ## MongoDB Operations
     * - UPDATE: Uses $pull to remove entity reference from meal document
     * - DELETE: Removes entity document from its collection
     *
     * ## Cache Impact
     * - Evicts specific meal from "meals" cache
     * - Clears entire "allMeals" cache
     *
     * @param id The entity ID to delete
     * @param entity The entity to delete (for validation and meal reference)
     * @return The deleted entity
     * @throws RuntimeException If user is not authorized to delete
     */
    open fun delete(id: ID, entity: T): T {
        if (userService.getCurrentUser().id != entity.userId && userService.getCurrentUser().role != Role.ADMIN) throw RuntimeException(
            "You are not allowed to delete this entity"
        )

        mongoTemplate.update(Meal::class.java).matching(Criteria.where("id").`is`(entity.mealId))
            .apply(Update().pull(entityFieldName, entity)).first()

        repository.deleteById(id)

        cacheManager.getCache("meals")!!.evict(entity.mealId)
        cacheManager.getCache("allMeals")!!.clear()

        return entity
    }

    /**
     * Finds an entity by user ID and meal ID.
     *
     * Used to enforce the one-entity-per-user-per-meal constraint during creation.
     * Implementations should query their specific collection for the entity.
     *
     * @param userId The user's ObjectId
     * @param mealId The meal's ObjectId
     * @return The entity if found, null otherwise
     */
    abstract fun findByUserId(userId: ObjectId, mealId: ObjectId): T?
    
    /**
     * The field name in the Meal document that stores this entity type's references.
     *
     * Used for MongoDB $push and $pull operations to maintain bidirectional relationships.
     * Examples: "ratings", "notes"
     *
     * @return MongoDB field name as string
     */
    abstract val entityFieldName: String
}

/**
 * Marker interface for entities that reference a meal and are owned by a user.
 *
 * Entities implementing this interface can be managed by [BaseService] for
 * automatic meal relationship and cache management.
 *
 * @property userId The owner's ObjectId (nullable for auto-assignment)
 * @property mealId The referenced meal's ObjectId
 */
interface EntityWithMealId {
    var userId: ObjectId?
    val mealId: ObjectId
}
//
//@Component
//class CachePrinter(@Autowired private val cacheManager: CacheManager) {
//
//    fun printCache() {
//        val cacheNames = cacheManager.cacheNames
//        println("Cache Content:")
//        cacheNames.forEach { cacheName ->
//            println("Cache Name: $cacheName")
//            val cache = cacheManager.getCache(cacheName)
//            cache?.nativeCache?.let { nativeCache ->
//                if (nativeCache is com.github.benmanes.caffeine.cache.Cache<*, *>) {
//                    nativeCache.asMap().forEach { (key, value) ->
//                        println("Key: $key, Value: $value")
//                    }
//                } else {
//                    println("Cache type ${nativeCache.javaClass.name} not supported for direct printing")
//                }
//            }
//        }
//    }
//}