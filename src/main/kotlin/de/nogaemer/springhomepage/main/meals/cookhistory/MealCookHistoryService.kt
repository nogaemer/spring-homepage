package de.nogaemer.springhomepage.main.meals.cookhistory

import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.main.meals.MealRepository
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Service layer for meal cook history operations.
 *
 * Handles business logic for recording and retrieving user cooking history.
 * Integrates with daily meal plans to auto-complete plans when meals are logged.
 *
 * ## Core Responsibilities
 * - Record new cook history entries
 * - Retrieve paginated cook history
 * - Get last cook date for meal detail displays
 * - Auto-complete daily meal plans when meal is logged
 *
 * ## Integration Points
 * - Fetches meal details from MealRepository for denormalization
 * - Updates DailyMealPlan completion status when meal is logged
 * - Validates meal existence before creating history entries
 *
 * @property cookHistoryRepository Repository for cook history persistence
 * @property dailyMealPlanRepository Repository for daily meal plan queries/updates
 * @property mealRepository Repository for meal lookups
 *
 * @see MealCookHistory
 * @see MealCookHistoryDto
 * @see DailyMealPlan
 */
@Service
class MealCookHistoryService(
    private val cookHistoryRepository: MealCookHistoryRepository,
    private val dailyMealPlanRepository: DailyMealPlanRepository,
    private val mealRepository: MealRepository
) {
    private val logger = LoggerFactory.getLogger(MealCookHistoryService::class.java)

    /**
     * Records a new cook history entry for a user.
     *
     * Creates a MealCookHistory document with denormalized meal data and optional
     * cooking details. If a DailyMealPlan exists for today with this meal, it will
     * be automatically marked as completed.
     *
     * ## Process
     * 1. Fetch meal details from database to get name and image
     * 2. Create MealCookHistory entry with current timestamp
     * 3. Check for today's DailyMealPlan with same mealId
     * 4. If plan exists and not completed, mark it completed
     *
     * ## Validation
     * - Meal must exist (throws IdNotFoundException if not found)
     *
     * ## Side Effects
     * - Creates new MealCookHistory document
     * - May update DailyMealPlan.isCompleted and completedAt fields
     *
     * @param userId User identifier who cooked the meal
     * @param mealId Meal identifier that was cooked
     * @param portionSize Optional number of portions cooked
     * @param rating Optional rating (1-5 scale) for this cook
     * @param notes Optional user notes about this cook
     * @throws IdNotFoundException If meal with mealId does not exist
     */
    fun recordMealCooked(
        userId: String,
        mealId: String,
        portionSize: Int?,
        rating: Int?,
        notes: String?
    ) {
        logger.debug("Recording meal cooked - userId: $userId, mealId: $mealId")

        // Fetch meal details for denormalization
        val meal = mealRepository.findById(ObjectId(mealId))
            ?: throw IdNotFoundException("Meal not found with id: $mealId")

        // Get first image URL if available
        val mealImageUrl = meal.images?.firstOrNull()?.url

        // Create cook history entry
        val cookHistory = MealCookHistory(
            userId = userId,
            mealId = mealId,
            cookedAt = LocalDateTime.now(),
            mealName = meal.name,
            mealImageUrl = mealImageUrl,
            portionSize = portionSize,
            rating = rating,
            notes = notes
        )

        cookHistoryRepository.save(cookHistory)
        logger.info("Saved cook history entry for user: $userId, meal: ${meal.name}")

        // Check if there's a daily meal plan for today with this meal
        val today = java.time.LocalDate.now()
        val dailyPlan = dailyMealPlanRepository.findByUserIdAndPlannedDate(userId, today)

        if (dailyPlan != null && dailyPlan.mealId == mealId && !dailyPlan.isCompleted) {
            dailyPlan.isCompleted = true
            dailyPlan.completedAt = LocalDateTime.now()
            dailyMealPlanRepository.save(dailyPlan)
            logger.info("Auto-completed daily meal plan for user: $userId, date: $today")
        }
    }

    /**
     * Retrieves paginated cook history for a user.
     *
     * Returns all cook history entries for a user, sorted by cook date descending
     * (most recent first). Results are paginated for efficient loading.
     *
     * ## Pagination
     * - page: Zero-based page number
     * - pageSize: Number of entries per page
     *
     * ## Default Sorting
     * Results are always sorted by cookedAt DESC
     *
     * @param userId User identifier to filter by
     * @param page Zero-based page number (default: 0)
     * @param pageSize Number of entries per page (default: 20)
     * @return Page of cook history DTOs
     */
    fun getUserCookHistory(userId: String, page: Int = 0, pageSize: Int = 20): Page<MealCookHistoryDto> {
        logger.debug("Getting cook history - userId: $userId, page: $page, pageSize: $pageSize")

        val pageable: Pageable = PageRequest.of(page, pageSize)
        val historyPage = cookHistoryRepository.findByUserIdOrderByCookedAtDesc(userId, pageable)

        return historyPage.map { it.toDto() }
    }

    /**
     * Retrieves paginated cook history for a specific meal by a user.
     *
     * Returns all times a specific user has cooked a specific meal, sorted by
     * cook date descending. Useful for displaying cooking statistics on meal
     * detail pages.
     *
     * ## Use Cases
     * - "You've cooked this 5 times" on meal detail
     * - Personal rating history for a meal
     * - Notes from previous cooks
     *
     * @param userId User identifier to filter by
     * @param mealId Meal identifier to filter by
     * @param page Zero-based page number (default: 0)
     * @param pageSize Number of entries per page (default: 20)
     * @return Page of cook history DTOs for this meal
     */
    fun getMealSpecificHistory(
        userId: String,
        mealId: String,
        page: Int = 0,
        pageSize: Int = 20
    ): Page<MealCookHistoryDto> {
        logger.debug("Getting meal-specific history - userId: $userId, mealId: $mealId")

        val pageable: Pageable = PageRequest.of(page, pageSize)
        val historyPage = cookHistoryRepository.findByUserIdAndMealIdOrderByCookedAtDesc(userId, mealId, pageable)

        return historyPage.map { it.toDto() }
    }

    /**
     * Retrieves recent cook history entries for a user.
     *
     * Returns the most recent cook history entries up to the specified limit.
     * Used for activity feeds and quick history views.
     *
     * ## Performance
     * Uses pagination internally but returns a simple List for convenience.
     * For very large limits (>100), consider using getUserCookHistory() instead.
     *
     * @param userId User identifier to filter by
     * @param limit Maximum number of entries to return (default: 10)
     * @return List of recent cook history DTOs
     */
    fun getRecentHistory(userId: String, limit: Int = 10): List<MealCookHistoryDto> {
        logger.debug("Getting recent history - userId: $userId, limit: $limit")

        val pageable: Pageable = PageRequest.of(0, limit)
        val historyPage = cookHistoryRepository.findByUserIdOrderByCookedAtDesc(userId, pageable)

        return historyPage.content.map { it.toDto() }
    }

    /**
     * Gets the last cook date for a specific meal by a user.
     *
     * Returns the timestamp when the user most recently cooked this meal.
     * Returns null if the user has never cooked this meal.
     *
     * ## Use Cases
     * - Display "Last cooked: 5 days ago" on meal detail pages
     * - Calculate cooking frequency
     * - Meal recommendation algorithms
     *
     * ## Performance
     * Very efficient - uses compound index and returns first matching document.
     *
     * @param userId User identifier to filter by
     * @param mealId Meal identifier to filter by
     * @return Last cook timestamp, or null if never cooked
     */
    fun getLastCookDateForMeal(userId: String, mealId: String): LocalDateTime? {
        logger.debug("Getting last cook date - userId: $userId, mealId: $mealId")

        val lastCook = cookHistoryRepository.findFirstByUserIdAndMealIdOrderByCookedAtDesc(userId, mealId)
        return lastCook?.cookedAt
    }
}
