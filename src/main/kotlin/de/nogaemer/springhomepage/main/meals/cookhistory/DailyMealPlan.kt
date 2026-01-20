package de.nogaemer.springhomepage.main.meals.cookhistory

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Represents a daily meal plan stored in the MongoDB "daily_meal_plans" collection.
 *
 * This document tracks the meal a user plans to cook for a specific day. Each user
 * can only have one meal plan per day (enforced by unique compound index). When the
 * meal is cooked, the plan is marked as completed and linked to a cook history entry.
 *
 * ## MongoDB Indexes
 * - Unique compound index on (userId, plannedDate) - one plan per user per day
 * - Individual index on userId for user-specific queries
 * - Individual index on plannedDate for date-based queries and cleanup
 *
 * ## Lifecycle
 * 1. User marks a meal for today → creates DailyMealPlan with isCompleted=false
 * 2. User cooks meal → logs to MealCookHistory, sets isCompleted=true, sets completedAt
 * 3. Scheduled task auto-completes plans from past days that weren't manually logged
 *
 * ## Use Cases
 * - Daily meal planning and reminder
 * - Auto-logging of planned meals
 * - Tracking plan completion rate
 * - Daily cooking streaks and statistics
 *
 * @property id MongoDB ObjectId, auto-generated when saved
 * @property userId User identifier who created the plan
 * @property mealId Reference to the Meal document to cook
 * @property mealName Denormalized meal name for quick display
 * @property mealImageUrl Optional denormalized meal image URL
 * @property plannedDate Date when user plans to cook this meal
 * @property markedAt Timestamp when user selected this meal for the day
 * @property isCompleted Whether the meal was cooked (logged to history)
 * @property completedAt Optional timestamp when meal was logged to history
 *
 * @see DailyMealPlanRepository
 * @see DailyMealPlanService
 */
@Document(collection = "daily_meal_plans")
@CompoundIndex(name = "userId_plannedDate_unique_idx", def = "{'userId': 1, 'plannedDate': 1}", unique = true)
data class DailyMealPlan(
    @Indexed
    val userId: String,
    
    val mealId: String,
    
    val mealName: String,
    
    val mealImageUrl: String?,
    
    @Indexed
    val plannedDate: LocalDate,
    
    val markedAt: LocalDateTime,
    
    var isCompleted: Boolean = false,
    
    var completedAt: LocalDateTime? = null
) {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: String? = null
}
