package de.nogaemer.springhomepage.main.meals.cookhistory

import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.main.meals.MealService
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Service layer for daily meal planning operations.
 *
 * Handles business logic for creating, retrieving, and completing daily meal plans.
 * Enforces one meal plan per user per day constraint and integrates with cook history.
 *
 * ## Core Responsibilities
 * - Mark a meal for a specific date (replaces any existing plan)
 * - Retrieve meal plans for specific dates
 * - Clear meal plan without logging to history
 * - Auto-complete past incomplete plans (scheduled task)
 * - Manually complete meal plan (logs to history)
 *
 * ## Business Rules
 * - One plan per user per day (enforced by MongoDB unique index)
 * - Marking a new meal replaces existing plan for the day
 * - Plans from past days can be auto-completed by scheduled task
 * - Completing a plan logs it to cook history and marks isCompleted
 *
 * @property dailyMealPlanRepository Repository for daily meal plan persistence
 * @property mealService Service for meal lookups
 * @property cookHistoryService Service for recording cook history
 *
 * @see DailyMealPlan
 * @see DailyMealPlanDto
 * @see MealCookHistoryService
 */
@Service
class DailyMealPlanService(
    private val dailyMealPlanRepository: DailyMealPlanRepository,
    private val mealService: MealService,
    private val cookHistoryService: MealCookHistoryService
) {
    private val logger = LoggerFactory.getLogger(DailyMealPlanService::class.java)

    /**
     * Marks a meal as the plan for a specific date.
     *
     * Creates a new DailyMealPlan for the specified date with the given meal. If a plan
     * already exists for that date, it is deleted first (user can only have one plan
     * per day). The meal name and image are denormalized for quick display.
     *
     * ## Process
     * 1. Fetch meal basic info using lightweight projection
     * 2. Delete any existing plan for the specified date
     * 3. Create new DailyMealPlan with plannedDate
     * 4. Save and return the plan
     *
     * ## Validation
     * - Meal must exist (throws IdNotFoundException if not found)
     *
     * ## Business Rules
     * - Only one meal can be planned per day (enforced by deletion + unique index)
     * - New plan is always created with isCompleted = false
     *
     * @param userId User identifier creating the plan
     * @param mealId Meal identifier to plan
     * @param plannedDate Date when user plans to cook this meal
     * @return Created daily meal plan DTO
     * @throws IdNotFoundException If meal with mealId does not exist
     */
    fun markMealForDate(userId: String, mealId: String, plannedDate: LocalDate): DailyMealPlanDto {
        logger.debug("Marking meal for date - userId: $userId, mealId: $mealId, date: $plannedDate")

        // Fetch meal basic info using lightweight projection (avoids N+1 query)
        val mealInfo = mealService.getMealBasicInfo(ObjectId(mealId))

        // Delete any existing plan for this date (user can only mark one meal per day)
        dailyMealPlanRepository.deleteByUserIdAndPlannedDate(userId, plannedDate)
        logger.debug("Deleted existing plan for date if any - userId: $userId, date: $plannedDate")

        // Create new plan
        val dailyPlan = DailyMealPlan(
            userId = userId,
            mealId = mealId,
            mealName = mealInfo.name,
            mealImageUrl = mealInfo.imageUrl,
            plannedDate = plannedDate,
            markedAt = LocalDateTime.now(),
            isCompleted = false,
            completedAt = null
        )

        val savedPlan = dailyMealPlanRepository.save(dailyPlan)
        logger.info("Created daily meal plan for user: $userId, meal: ${mealInfo.name}, date: $plannedDate")

        return savedPlan.toDto()
    }

    /**
     * Marks a meal as the plan for today.
     *
     * Convenience method that calls markMealForDate with today's date.
     *
     * @param userId User identifier creating the plan
     * @param mealId Meal identifier to plan for today
     * @return Created daily meal plan DTO
     * @throws IdNotFoundException If meal with mealId does not exist
     */
    fun markMealForToday(userId: String, mealId: String): DailyMealPlanDto {
        return markMealForDate(userId, mealId, LocalDate.now())
    }

    /**
     * Retrieves meal plan for a specific date.
     *
     * Returns the meal plan for the specified date if it exists. Returns null if no plan
     * is set for that date.
     *
     * @param userId User identifier to filter by
     * @param date Date to retrieve plan for
     * @return Meal plan DTO, or null if no plan exists
     */
    fun getMealPlanForDate(userId: String, date: LocalDate): DailyMealPlanDto? {
        logger.debug("Getting meal plan for date - userId: $userId, date: $date")

        val dailyPlan = dailyMealPlanRepository.findByUserIdAndPlannedDate(userId, date)

        return dailyPlan?.toDto()
    }

    /**
     * Retrieves today's meal plan for a user.
     *
     * Returns the meal plan for today if it exists. Returns null if no plan
     * is set for today.
     *
     * ## Auto-Completion
     * This method does NOT auto-complete past plans. That is handled by the
     * scheduled task. This method only returns today's plan.
     *
     * @param userId User identifier to filter by
     * @return Today's meal plan DTO, or null if no plan exists
     */
    fun getTodaysMealPlan(userId: String): DailyMealPlanDto? {
        logger.debug("Getting today's meal plan - userId: $userId")

        val today = LocalDate.now()
        val dailyPlan = dailyMealPlanRepository.findByUserIdAndPlannedDate(userId, today)

        return dailyPlan?.toDto()
    }

    /**
     * Clears today's meal plan without logging to history.
     *
     * Deletes the meal plan for today if it exists. This is used when the user
     * changes their mind and doesn't want to cook the planned meal. Does NOT
     * create a cook history entry.
     *
     * ## Use Case
     * User marked a meal for today but decided not to cook it.
     *
     * @param userId User identifier whose plan to clear
     */
    fun clearMealPlan(userId: String) {
        logger.debug("Clearing today's meal plan - userId: $userId")

        val today = LocalDate.now()
        dailyMealPlanRepository.deleteByUserIdAndPlannedDate(userId, today)

        logger.info("Cleared meal plan for user: $userId, date: $today")
    }

    /**
     * Auto-completes past incomplete meal plans.
     *
     * Finds all DailyMealPlan entries where plannedDate < today and isCompleted = false,
     * then logs them to cook history and marks them as completed. This handles cases
     * where users forgot to manually log their meals.
     *
     * ## Scheduled Task
     * This method should be called by a scheduled task (e.g., daily at 01:00 AM)
     * to clean up past plans.
     *
     * ## Process
     * 1. Find all incomplete plans from past dates
     * 2. For each plan:
     *    - Call recordMealCooked to create history entry
     *    - Mark plan as completed with completedAt timestamp
     *    - Save updated plan
     *
     * ## Logging
     * Logs to cook history with null values for portionSize, rating, and notes
     * since these details weren't manually provided.
     *
     * ## Concurrency Note
     * This method processes plans individually in a forEach loop. In distributed
     * deployments with multiple instances, consider implementing distributed locking
     * (e.g., using Redis or database-based locks) to prevent duplicate processing.
     */
    fun autoCompletePastPlans() {
        logger.info("Starting auto-completion of past meal plans")

        val today = LocalDate.now()
        val incompletePlans = dailyMealPlanRepository.findByPlannedDateBeforeAndIsCompletedFalse(today)

        logger.info("Found ${incompletePlans.size} incomplete plans to auto-complete")

        incompletePlans.forEach { plan ->
            try {
                // Log to cook history (this will also mark the plan as completed)
                cookHistoryService.recordMealCooked(
                    userId = plan.userId,
                    mealId = plan.mealId
                )
                logger.info(
                    "Auto-completed plan - userId: ${plan.userId}, meal: ${plan.mealName}, " +
                            "plannedDate: ${plan.plannedDate}"
                )
            } catch (e: Exception) {
                logger.error(
                    "Failed to auto-complete plan - userId: ${plan.userId}, " +
                            "mealId: ${plan.mealId}, error: ${e.message}", e
                )
            }
        }

        logger.info("Completed auto-completion of ${incompletePlans.size} past meal plans")
    }

    /**
     * Manually completes today's meal plan.
     *
     * Logs the meal to cook history and marks today's plan as completed. This is
     * used when the user explicitly marks the meal as cooked through the API.
     *
     * ## Process
     * 1. Retrieve today's plan (if exists)
     * 2. Call recordMealCooked to create history entry (which also updates the plan)
     *
     * ## Validation
     * - Plan must exist for today
     *
     * @param userId User identifier completing the plan
     * @throws IdNotFoundException If no plan exists for today
     */
    fun completeMealPlan(userId: String) {
        logger.debug("Completing meal plan - userId: $userId")

        val today = LocalDate.now()
        val dailyPlan = dailyMealPlanRepository.findByUserIdAndPlannedDate(userId, today)
            ?: throw IdNotFoundException("No meal plan found for today")

        // Record meal cooked (this will also mark the plan as completed)
        cookHistoryService.recordMealCooked(
            userId = userId,
            mealId = dailyPlan.mealId
        )

        logger.info("Completed meal plan for user: $userId, meal: ${dailyPlan.mealName}")
    }
}
