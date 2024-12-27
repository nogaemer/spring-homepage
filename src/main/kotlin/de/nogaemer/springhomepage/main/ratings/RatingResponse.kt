package de.nogaemer.springhomepage.main.ratings

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.user.Role
import de.nogaemer.springhomepage.user.UserResponse
import org.bson.types.ObjectId


data class RatingResponse(
    val rating: Rating,
    val user: UserResponse,
    val mealRating: Double,
)
