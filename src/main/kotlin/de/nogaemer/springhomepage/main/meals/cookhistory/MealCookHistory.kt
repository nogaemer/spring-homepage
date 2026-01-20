package de.nogaemer.springhomepage.main.meals.cookhistory

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * Represents a cook history entry stored in the MongoDB "meal_cook_history" collection.
 *
 * This document tracks every time a user cooks a meal, including optional details
 * like portion size, rating, and personal notes. The data is denormalized to include
 * meal name and image URL for quick display without additional lookups.
 *
 * ## MongoDB Indexes
 * - Compound index on (userId ASC, cookedAt DESC) for efficient user history queries
 * - Individual index on cookedAt DESC for sorting
 *
 * ## Use Cases
 * - Track cooking frequency and patterns
 * - Display "Last cooked: X days ago" on meal detail pages
 * - Generate cooking statistics and recommendations
 * - Rate and review meals after cooking
 *
 * @property id MongoDB ObjectId, auto-generated when saved
 * @property userId User identifier who cooked the meal
 * @property mealId Reference to the Meal document
 * @property cookedAt Timestamp when the meal was cooked
 * @property mealName Denormalized meal name for quick display
 * @property mealImageUrl Optional denormalized meal image URL
 * @property portionSize Optional number of portions cooked (may differ from recipe)
 * @property rating Optional user rating (1-5 scale) for this specific cook
 * @property notes Optional user notes about this cooking experience
 *
 * @see MealCookHistoryRepository
 * @see MealCookHistoryService
 */
@Document(collection = "meal_cook_history")
@CompoundIndex(name = "userId_cookedAt_idx", def = "{'userId': 1, 'cookedAt': -1}")
data class MealCookHistory(
    @Indexed
    val userId: String,
    
    val mealId: String,
    
    @Indexed(direction = org.springframework.data.mongodb.core.index.IndexDirection.DESCENDING)
    val cookedAt: LocalDateTime,
    
    val mealName: String,
    
    val mealImageUrl: String?,
    
    val portionSize: Int?,
    
    val rating: Int?,
    
    val notes: String?
) {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: String? = null
}
