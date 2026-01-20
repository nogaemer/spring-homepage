package de.nogaemer.springhomepage.main.meals.dto

import de.nogaemer.springhomepage.main.images.Image
import de.nogaemer.springhomepage.main.meals.models.Meal
import de.nogaemer.springhomepage.main.meals.models.MealIngredient
import de.nogaemer.springhomepage.main.notes.Note
import de.nogaemer.springhomepage.main.ratings.Rating
import de.nogaemer.springhomepage.main.tags.Tag
import java.time.LocalDateTime

/**
 * Extended Data Transfer Object for meal details including cook history information.
 *
 * This DTO extends the standard Meal entity by adding cooking history metadata.
 * Used when returning detailed meal information to authenticated users.
 *
 * ## Additional Fields
 * - **lastCookedAt**: Timestamp when the current user last cooked this meal
 *
 * ## Use Cases
 * - Meal detail pages showing "Last cooked: X days ago"
 * - Personalized meal recommendations based on cooking frequency
 * - User-specific meal statistics
 *
 * @property meal The complete meal entity with all details
 * @property lastCookedAt Optional timestamp of last cook by current user
 *
 * @see Meal
 */
data class MealWithCookHistoryDto(
    val id: String?,
    val name: String,
    val ingredients: List<MealIngredient>,
    val instructions: List<String>,
    val images: List<Image>?,
    val difficulty: String,
    val time: Long,
    val portions: Int,
    val calories: Int,
    val url: String,
    val tags: MutableList<Tag>,
    val rating: Double,
    val ratings: List<Rating>,
    val notes: List<Note>,
    val lastCookedAt: LocalDateTime?
)

/**
 * Extension function to convert [Meal] to [MealWithCookHistoryDto].
 *
 * Creates a DTO including all meal details plus optional cooking history.
 *
 * @param lastCookedAt Optional timestamp when user last cooked this meal
 * @return DTO with meal details and cook history
 */
fun Meal.toMealWithCookHistory(lastCookedAt: LocalDateTime?) = MealWithCookHistoryDto(
    id = id?.toString(),
    name = name,
    ingredients = ingredients,
    instructions = instructions,
    images = images,
    difficulty = difficulty,
    time = time,
    portions = portions,
    calories = calories,
    url = url,
    tags = tags,
    rating = rating,
    ratings = ratings,
    notes = notes,
    lastCookedAt = lastCookedAt
)
