package de.nogaemer.springhomepage.main.meals.cookhistory

import de.nogaemer.springhomepage.user.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for daily meal planning.
 *
 * Provides endpoints for managing user's daily meal plans - marking meals for
 * today, retrieving today's plan, clearing plans, and completing plans.
 * All endpoints require authentication to identify the current user.
 * All endpoints are mapped under /api/v1/meal-plan base path.
 *
 * ## Endpoint Summary
 * - POST /api/v1/meal-plan/mark-today - Mark a meal for today
 * - GET /api/v1/meal-plan/today - Get today's meal plan
 * - DELETE /api/v1/meal-plan/today - Clear today's plan
 * - POST /api/v1/meal-plan/complete-today - Mark today's plan as cooked
 *
 * ## Authentication
 * All endpoints require valid authentication. User ID is extracted from
 * Spring Security context via UserService.getCurrentUser().
 *
 * ## Business Rules
 * - One meal plan per user per day
 * - Marking a new meal replaces existing plan
 * - Clearing plan does NOT log to history
 * - Completing plan DOES log to history
 *
 * @property dailyMealPlanService Service for daily meal plan operations
 * @property userService Service for retrieving current authenticated user
 *
 * @see DailyMealPlanService
 * @see DailyMealPlanDto
 */
@RestController
@RequestMapping("/api/v1/meal-plan")
class DailyMealPlanController(
    private val dailyMealPlanService: DailyMealPlanService,
    private val userService: UserService
) {

    /**
     * POST /api/v1/meal-plan/mark-today
     *
     * Marks a meal as the plan for today.
     *
     * Creates a new daily meal plan for the current date with the specified meal.
     * If a plan already exists for today, it is replaced (user can only have one
     * plan per day).
     *
     * ## Request Body
     * ```json
     * {
     *   "mealId": "507f1f77bcf86cd799439011"
     * }
     * ```
     *
     * ## Response
     * - **200 OK**: [DailyMealPlanDto] with created plan details
     * - **404 Not Found**: If meal with mealId does not exist
     *
     * ## Business Rules
     * - Replaces any existing plan for today
     * - Plan is created with isCompleted = false
     * - Meal name and image are denormalized for quick display
     *
     * ## Authentication Required
     * User must be authenticated. User ID is extracted from security context.
     *
     * @param request Request containing mealId to mark for today
     * @return ResponseEntity containing created DailyMealPlanDto
     * @throws IdNotFoundException If meal does not exist
     */
    @PostMapping("/mark-today")
    fun markMealForToday(
        @RequestBody request: MarkMealForTodayRequest
    ): ResponseEntity<DailyMealPlanDto> {
        val userId = userService.getCurrentUser().id!!.toString()

        val plan = dailyMealPlanService.markMealForToday(userId, request.mealId)

        return ResponseEntity.ok(plan)
    }

    /**
     * GET /api/v1/meal-plan/today
     *
     * Retrieves today's meal plan for the authenticated user.
     *
     * Returns the meal plan for the current date if one exists. Returns 404
     * if no plan is set for today.
     *
     * ## Response
     * - **200 OK**: [DailyMealPlanDto] with today's plan details
     * - **404 Not Found**: If no plan exists for today
     *
     * ## Use Cases
     * - Display "Today's Meal" on dashboard
     * - Show meal reminder notifications
     * - Quick access to planned meal details
     *
     * ## Authentication Required
     * User must be authenticated. User ID is extracted from security context.
     *
     * @return ResponseEntity containing DailyMealPlanDto or 404 if no plan
     */
    @GetMapping("/today")
    fun getTodaysMealPlan(): ResponseEntity<DailyMealPlanDto> {
        val userId = userService.getCurrentUser().id!!.toString()

        val plan = dailyMealPlanService.getTodaysMealPlan(userId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(plan)
    }

    /**
     * DELETE /api/v1/meal-plan/today
     *
     * Clears today's meal plan without logging to history.
     *
     * Deletes the meal plan for today if it exists. This is used when the user
     * changes their mind and doesn't want to cook the planned meal. Does NOT
     * create a cook history entry.
     *
     * ## Response
     * - **200 OK**: Plan cleared successfully (or no plan existed)
     *
     * ## Use Case
     * User marked a meal for today but decided not to cook it.
     *
     * ## Business Rules
     * - Does NOT log to cook history
     * - Idempotent - safe to call even if no plan exists
     *
     * ## Authentication Required
     * User must be authenticated. User ID is extracted from security context.
     *
     * @return ResponseEntity with 200 OK status
     */
    @DeleteMapping("/today")
    fun clearMealPlan(): ResponseEntity<Void> {
        val userId = userService.getCurrentUser().id!!.toString()

        dailyMealPlanService.clearMealPlan(userId)

        return ResponseEntity.ok().build()
    }

    /**
     * POST /api/v1/meal-plan/complete-today
     *
     * Marks today's meal plan as cooked.
     *
     * Logs the planned meal to cook history and marks the plan as completed.
     * This is used when the user explicitly confirms they cooked today's planned meal.
     *
     * ## Response
     * - **200 OK**: Meal logged to history and plan marked completed
     * - **404 Not Found**: If no plan exists for today
     *
     * ## Process
     * 1. Verify today's plan exists
     * 2. Create cook history entry
     * 3. Mark plan as completed (isCompleted = true, completedAt = now)
     *
     * ## Business Rules
     * - DOES log to cook history (unlike clearMealPlan)
     * - Plan must exist for today
     * - Plan is marked as completed
     *
     * ## Authentication Required
     * User must be authenticated. User ID is extracted from security context.
     *
     * @return ResponseEntity with 200 OK status
     * @throws IdNotFoundException If no plan exists for today
     */
    @PostMapping("/complete-today")
    fun completeMealPlan(): ResponseEntity<Void> {
        val userId = userService.getCurrentUser().id!!.toString()

        // Get today's plan to get the mealId
        val plan = dailyMealPlanService.getTodaysMealPlan(userId)
            ?: throw de.nogaemer.springhomepage.exceptions.IdNotFoundException("No meal plan found for today")

        dailyMealPlanService.completeMealPlan(userId, plan.mealId)

        return ResponseEntity.ok().build()
    }
}

/**
 * Request DTO for marking a meal for today.
 *
 * @property mealId MongoDB ObjectId of the meal to mark for today
 */
data class MarkMealForTodayRequest(
    val mealId: String
)
