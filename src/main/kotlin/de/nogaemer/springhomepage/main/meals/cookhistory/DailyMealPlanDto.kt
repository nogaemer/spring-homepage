package de.nogaemer.springhomepage.main.meals.cookhistory

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Data Transfer Object for [DailyMealPlan] entity.
 *
 * Used for API responses when returning daily meal plan data to clients.
 * Contains all relevant information for displaying a meal plan without
 * exposing internal MongoDB document structure.
 *
 * ## Use Cases
 * - GET /api/v1/meal-plan/today endpoint responses
 * - Meal plan creation confirmations
 * - Meal plan update responses
 *
 * @property id Meal plan identifier
 * @property userId User who created the plan
 * @property mealId Reference to the meal planned to cook
 * @property mealName Name of the meal (denormalized for display)
 * @property mealImageUrl Optional image URL for the meal
 * @property plannedDate Date when meal is planned to be cooked
 * @property markedAt Timestamp when plan was created
 * @property isCompleted Whether the meal was cooked
 * @property completedAt Optional timestamp when meal was logged to history
 *
 * @see DailyMealPlan
 */
data class DailyMealPlanDto(
    val id: String?,
    val userId: String,
    val mealId: String,
    val mealName: String,
    val mealImageUrl: String?,
    val plannedDate: LocalDate,
    val markedAt: LocalDateTime,
    val isCompleted: Boolean,
    val completedAt: LocalDateTime?
)

/**
 * Extension function to convert [DailyMealPlan] entity to [DailyMealPlanDto].
 *
 * Provides clean mapping from domain model to data transfer object for API responses.
 * Uses Kotlin's extension function pattern for idiomatic code.
 *
 * ## Usage
 * ```kotlin
 * val plan: DailyMealPlan = repository.findByUserIdAndPlannedDate(userId, today)
 * val dto: DailyMealPlanDto = plan.toDto()
 * ```
 *
 * @return DTO representation of the daily meal plan
 */
fun DailyMealPlan.toDto() = DailyMealPlanDto(
    id = id,
    userId = userId,
    mealId = mealId,
    mealName = mealName,
    mealImageUrl = mealImageUrl,
    plannedDate = plannedDate,
    markedAt = markedAt,
    isCompleted = isCompleted,
    completedAt = completedAt
)
