package de.nogaemer.springhomepage.main.meals

import de.nogaemer.springhomepage.main.meals.models.Meal
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

/**
 * MongoDB repository interface for [Meal] entity persistence and queries.
 *
 * Extends Spring Data MongoRepository to provide standard CRUD operations plus
 * custom query methods for meal-specific data access patterns.
 *
 * ## Standard Operations
 * Inherited from MongoRepository:
 * - save(): Insert or update meal
 * - findAll(): Retrieve all meals (not recommended for large datasets)
 * - deleteById(): Delete meal by ID
 * - count(): Count total meals
 *
 * ## Custom Queries
 * - [searchByName]: Case-insensitive regex search on meal names
 * - [findByName]: Exact name match (case-insensitive)
 * - [findByUrl]: Find imported meals by source URL
 * - [countByUrl]: Check for duplicate imports
 *
 * ## Indexing Recommendations
 * For optimal query performance, consider these MongoDB indexes:
 * - `name`: Text index for full-text search or regular index for prefix matching
 * - `url`: Regular index for import duplicate detection
 * - `tags`: Multi-key index for tag-based filtering
 * - `rating`: Regular index for sorting by rating
 * - `time`: Regular index for filtering by cooking time
 *
 * ## Usage Notes
 * - Complex queries with aggregations are implemented in [MealService] rather than
 *   as repository methods to leverage MongoTemplate for advanced operations
 * - The [@Query] annotation uses MongoDB query syntax directly
 * - Case-insensitive searches use regex with 'i' option
 *
 * @see Meal
 * @see MealService
 */
@Repository
interface MealRepository : MongoRepository<Meal, ObjectId> {

    /**
     * Searches for meals with names matching the provided pattern (case-insensitive).
     *
     * Uses MongoDB regex query with case-insensitive flag for partial name matching.
     * Useful for autocomplete and search-as-you-type features.
     *
     * @param name Search pattern (treated as regex, e.g., "pasta" matches "Pasta Carbonara")
     * @return List of matching meals, or null if none found
     */
    @Query("{ 'name' : { \$regex: ?0, \$options: 'i' } }")
    fun searchByName(name: String?): List<Meal>?

    /**
     * Finds a meal by its ObjectId.
     *
     * Note: This method duplicates the inherited findById() but returns Meal? instead
     * of Optional<Meal>. Prefer using the inherited findById() method for consistency.
     *
     * @param id The MongoDB ObjectId of the meal
     * @return The meal if found, null otherwise
     */
    fun findById(id: ObjectId?): Meal?

    /**
     * Finds a meal by exact name match (case-insensitive via MongoDB collation).
     *
     * Used to prevent duplicate meal names during creation. The case-insensitive
     * behavior depends on the MongoDB collection's collation settings.
     *
     * @param name Exact name to search for
     * @return The meal if found, null otherwise
     */
    fun findByName(name: String?): Meal?

    /**
     * Finds a meal by its source URL.
     *
     * Used to locate meals that were imported from external recipe websites.
     * Returns null for meals created manually (which have empty or null URLs).
     *
     * @param url The complete source URL of the imported recipe
     * @return The meal if found, null otherwise
     */
    fun findByUrl(url: String?): Meal?

    /**
     * Counts meals with the specified source URL.
     *
     * Used during import operations to detect duplicate imports from the same URL.
     * Prevents importing the same recipe multiple times.
     *
     * ## Query Structure
     * Uses explicit MongoDB query syntax with count=true for optimized counting.
     *
     * @param url The source URL to check for duplicates
     * @return Number of meals with this URL (0 if none, ≥1 if duplicates exist)
     */
    @Query(value = " {'url': ?0} ", count = true)
    fun countByUrl(url: String?): Int
}