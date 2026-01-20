package de.nogaemer.springhomepage.main.meals.cookhistory

import java.time.LocalDateTime

/**
 * Data Transfer Object for [MealCookHistory] entity.
 *
 * Used for API responses when returning cook history data to clients.
 * Contains all relevant information for displaying a cook history entry
 * without exposing internal MongoDB document structure.
 *
 * ## Use Cases
 * - GET /api/v1/history endpoint responses
 * - Cook history lists and timelines
 * - Activity feeds and statistics
 *
 * @property id Cook history entry identifier
 * @property mealId Reference to the meal that was cooked
 * @property mealName Name of the meal (denormalized for display)
 * @property mealImageUrl Optional image URL for the meal
 * @property cookedAt Timestamp when the meal was cooked
 *
 * @see MealCookHistory
 */
data class MealCookHistoryDto(
    val id: String?,
    val mealId: String,
    val mealName: String,
    val mealImageUrl: String?,
    val cookedAt: LocalDateTime
)

/**
 * Extension function to convert [MealCookHistory] entity to [MealCookHistoryDto].
 *
 * Provides clean mapping from domain model to data transfer object for API responses.
 * Uses Kotlin's extension function pattern for idiomatic code.
 *
 * ## Usage
 * ```kotlin
 * val history: MealCookHistory = repository.findById(id)
 * val dto: MealCookHistoryDto = history.toDto()
 * ```
 *
 * @return DTO representation of the cook history entry
 */
fun MealCookHistory.toDto() = MealCookHistoryDto(
    id = id,
    mealId = mealId,
    mealName = mealName,
    mealImageUrl = mealImageUrl,
    cookedAt = cookedAt
)
