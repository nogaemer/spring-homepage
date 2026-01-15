/**
 * Response DTOs for rating endpoints.
 *
 * These classes structure rating data with associated user information for API responses.
 */
package de.nogaemer.springhomepage.main.ratings

import de.nogaemer.springhomepage.user.UserResponse

/**
 * Complete rating response for a meal including all individual ratings and average.
 *
 * @property ratings List of individual user ratings with user details
 * @property mealRating Average rating score for the meal
 */
data class RatingResponse(
    val ratings: MutableList<UserMealRatingResponse>,
    val mealRating: Double,
)

/**
 * Individual rating with associated user information.
 *
 * @property rating The rating data
 * @property user User information for the rating author
 */
data class UserMealRatingResponse(
    val rating: Rating,
    val user: UserResponse,
)
