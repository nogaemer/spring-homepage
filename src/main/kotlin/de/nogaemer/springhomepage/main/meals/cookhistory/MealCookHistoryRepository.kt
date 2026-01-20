package de.nogaemer.springhomepage.main.meals.cookhistory

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * MongoDB repository interface for [MealCookHistory] entity persistence and queries.
 *
 * Provides CRUD operations plus custom query methods for cook history access patterns.
 * All queries leverage the compound index (userId, cookedAt DESC) for optimal performance.
 *
 * ## Query Patterns
 * - User's complete cook history (paginated)
 * - Meal-specific cook history for a user (paginated)
 * - Recent cooks for activity feeds
 * - Last cook date for "Last cooked X days ago" display
 * - History before a specific date (for data retention/archival)
 *
 * ## Performance Notes
 * - All queries use the compound index for efficient filtering and sorting
 * - Paginated queries return Page<T> for large result sets
 * - Use findFirst for single-record lookups (most recent cook)
 *
 * @see MealCookHistory
 * @see MealCookHistoryService
 */
@Repository
interface MealCookHistoryRepository : MongoRepository<MealCookHistory, String> {
    
    /**
     * Finds all cook history entries for a user, sorted by cook date descending.
     *
     * Used for displaying a user's complete cooking history with pagination support.
     * Most recent cooks appear first.
     *
     * ## Pagination
     * Use PageRequest.of(page, size) to control results:
     * - page: Zero-based page number
     * - size: Number of records per page
     *
     * ## Index Usage
     * Uses compound index (userId ASC, cookedAt DESC)
     *
     * @param userId User identifier to filter by
     * @param pageable Pagination and sorting parameters
     * @return Page of cook history entries sorted by date descending
     */
    fun findByUserIdOrderByCookedAtDesc(userId: String, pageable: Pageable): Page<MealCookHistory>
    
    /**
     * Finds cook history for a specific meal by a specific user.
     *
     * Used for displaying "how many times have I cooked this?" on meal detail pages.
     * Results are sorted with most recent cook first.
     *
     * ## Use Cases
     * - Meal-specific cooking statistics
     * - Meal-specific cooking frequency and recency ("When did I last cook this?")
     * - Timeline of this user's cooks of the meal, including associated images where available
     *
     * ## Index Usage
     * Uses compound index (userId ASC, cookedAt DESC) plus meal filter
     *
     * @param userId User identifier to filter by
     * @param mealId Meal identifier to filter by
     * @param pageable Pagination and sorting parameters
     * @return Page of cook history entries for this meal sorted by date descending
     */
    fun findByUserIdAndMealIdOrderByCookedAtDesc(userId: String, mealId: String, pageable: Pageable): Page<MealCookHistory>
    
    /**
     * Finds cook history entries before a specific date.
     *
     * Used for data retention policies, archival, or cleanup of old history.
     * Can be used to implement "delete history older than X months" features.
     *
     * ## Use Cases
     * - Data archival processes
     * - GDPR data retention compliance
     * - Database cleanup tasks
     *
     * @param userId User identifier to filter by
     * @param date Cut-off date (entries before this date are returned)
     * @return List of cook history entries before the specified date
     */
    fun findByUserIdAndCookedAtBefore(userId: String, date: LocalDateTime): List<MealCookHistory>
    
    /**
     * Finds the most recent cook of a specific meal by a specific user.
     *
     * Used to display "Last cooked: 5 days ago" on meal detail pages.
     * Returns null if the user has never cooked this meal.
     *
     * ## Performance
     * Uses compound index and returns immediately after finding first match.
     * Very efficient for single-record lookups.
     *
     * @param userId User identifier to filter by
     * @param mealId Meal identifier to filter by
     * @return Most recent cook history entry, or null if never cooked
     */
    fun findFirstByUserIdAndMealIdOrderByCookedAtDesc(userId: String, mealId: String): MealCookHistory?
}
