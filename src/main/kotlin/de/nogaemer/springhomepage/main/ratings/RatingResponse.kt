package de.nogaemer.springhomepage.main.ratings

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.user.Role
import de.nogaemer.springhomepage.user.UserResponse
import org.apache.commons.lang3.mutable.Mutable
import org.bson.types.ObjectId


data class RatingResponse(
    val ratings: MutableList<UserMealRatingResponse>,
    val mealRating: Double,
)

data class UserMealRatingResponse(
    val rating: Rating,
    val user: UserResponse
)
