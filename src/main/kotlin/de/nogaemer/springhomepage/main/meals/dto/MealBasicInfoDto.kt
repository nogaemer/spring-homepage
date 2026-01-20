package de.nogaemer.springhomepage.main.meals.dto

/**
 * Lightweight Data Transfer Object for meal basic info.
 *
 * Contains only essential fields (name and image) for scenarios where full meal
 * details are not needed, avoiding N+1 query issues from DocumentReference loading.
 *
 * ## Use Cases
 * - Denormalizing meal data in cook history
 * - Denormalizing meal data in meal plans
 * - Any scenario requiring just meal name and image without related entities
 *
 * @property name Display name of the meal
 * @property imageUrl Optional first image URL for display
 *
 * @see de.nogaemer.springhomepage.main.meals.models.Meal
 */
data class MealBasicInfoDto(
    val name: String,
    val imageUrl: String?
)
