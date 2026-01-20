package de.nogaemer.springhomepage.main.meals.cookhistory

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Configuration class to enable Spring's scheduled task execution.
 *
 * This configuration enables the @Scheduled annotation support across the application,
 * allowing methods annotated with @Scheduled to run at specified intervals.
 *
 * ## Scheduling Support
 * Enables:
 * - Cron-based scheduling
 * - Fixed rate execution
 * - Fixed delay execution
 * - Initial delay configuration
 *
 * @see Scheduled
 * @see MealPlanScheduledTasks
 */
@Configuration
@EnableScheduling
class SchedulingConfig

/**
 * Scheduled tasks for daily meal plan auto-completion.
 *
 * Runs background tasks to automatically complete meal plans from past days
 * that were never manually logged by users.
 *
 * ## Schedule
 * Runs daily at 01:00 AM server time to clean up incomplete plans from
 * previous days.
 *
 * ## Purpose
 * - Auto-log meals that users planned but forgot to mark as cooked
 * - Maintain data consistency
 * - Generate complete cooking history for statistics
 *
 * ## Error Handling
 * Exceptions are caught and logged to prevent task failure from affecting
 * other scheduled tasks or application stability.
 *
 * @property dailyMealPlanService Service for meal plan operations
 *
 * @see DailyMealPlanService.autoCompletePastPlans
 */
@Component
class MealPlanScheduledTasks(
    private val dailyMealPlanService: DailyMealPlanService
) {
    private val logger = LoggerFactory.getLogger(MealPlanScheduledTasks::class.java)

    /**
     * Auto-completes past incomplete meal plans.
     *
     * Runs daily at 01:00 AM to find all meal plans from past dates where
     * isCompleted = false, then logs them to cook history and marks them completed.
     *
     * ## Schedule
     * - Cron: "0 0 1 * * *" (Every day at 01:00:00 AM)
     * - Timezone: Server timezone
     *
     * ## Process
     * 1. Find all plans where plannedDate < today and isCompleted = false
     * 2. For each plan:
     *    - Log to cook history with auto-generated note
     *    - Mark plan as completed
     *    - Save updated plan
     *
     * ## Error Handling
     * Individual plan failures are logged but don't stop processing of
     * remaining plans. Overall failures are caught and logged.
     *
     * ## Performance
     * Expected load: Low (typically 0-100 plans per day)
     * Execution time: <1 second for typical workloads
     */
    @Scheduled(cron = "0 0 1 * * *")
    fun autoCompletePastMealPlans() {
        try {
            logger.info("Starting scheduled auto-completion of past meal plans")
            dailyMealPlanService.autoCompletePastPlans()
            logger.info("Completed scheduled auto-completion of past meal plans")
        } catch (e: Exception) {
            logger.error("Error during auto-completion of past meal plans", e)
        }
    }
}
