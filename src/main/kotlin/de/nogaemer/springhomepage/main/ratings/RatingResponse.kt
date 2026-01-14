package de.nogaemer.springhomepage.main.ratings

import de.nogaemer.springhomepage.user.UserResponse


data class RatingResponse(
    val ratings: MutableList<UserMealRatingResponse>,
    val mealRating: Double,
)

data class UserMealRatingResponse(
    val rating: Rating,
    val user: UserResponse,
)
