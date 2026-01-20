package de.nogaemer.springhomepage.main.meals.cookhistory

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * MongoDB repository interface for [DailyMealPlan] entity persistence and queries.
 *
 * Provides CRUD operations plus custom query methods for daily meal planning.
 * All queries leverage indexes on userId, plannedDate, and the unique compound
 * index (userId, plannedDate).
 *
 * ## Query Patterns
 * - Get today's meal plan for a user
 * - Delete/update today's plan
 * - Find incomplete plans from past dates (for auto-completion)
 *
 * ## Uniqueness Constraint
 * The compound index (userId, plannedDate) is unique, enforcing one plan per user
 * per day. Attempting to insert a duplicate will throw DuplicateKeyException.
 *
 * @see DailyMealPlan
 * @see DailyMealPlanService
 */
@Repository
interface DailyMealPlanRepository : MongoRepository<DailyMealPlan, String> {
    
    /**
     * Finds a meal plan for a specific user and date.
     *
     * Used to retrieve today's meal plan or check if a plan exists for a specific date.
     * Returns null if no plan exists.
     *
     * ## Index Usage
     * Uses unique compound index (userId, plannedDate)
     *
     * ## Use Cases
     * - Get today's meal plan for display
     * - Check if user already has a plan for today before creating
     * - Retrieve historical plans
     *
     * @param userId User identifier to filter by
     * @param date Planned date to filter by
     * @return Meal plan for the specified user and date, or null if not found
     */
    fun findByUserIdAndPlannedDate(userId: String, date: LocalDate): DailyMealPlan?
    
    /**
     * Deletes a meal plan for a specific user and date.
     *
     * Used when user wants to clear their meal plan without logging it to history.
     * This is a "changed my mind" operation.
     *
     * ## Index Usage
     * Uses unique compound index (userId, plannedDate)
     *
     * ## Transaction Safety
     * Delete operations in MongoDB are atomic for single documents.
     *
     * @param userId User identifier to filter by
     * @param date Planned date to filter by
     */
    fun deleteByUserIdAndPlannedDate(userId: String, date: LocalDate)
    
    /**
     * Finds all incomplete meal plans before a specific date.
     *
     * Used by scheduled task to auto-complete plans from past days that were
     * never manually logged. For example, plans from yesterday that user forgot
     * to mark as completed.
     *
     * ## Scheduled Task Usage
     * Called daily (e.g., at midnight) to find plans from previous days where
     * isCompleted=false, then auto-logs them to cook history.
     *
     * ## Index Usage
     * Uses index on plannedDate plus filter on isCompleted
     *
     * @param date Cut-off date (plans before this date are returned)
     * @return List of incomplete plans from past dates
     */
    fun findByPlannedDateBeforeAndIsCompletedFalse(date: LocalDate): List<DailyMealPlan>
}
