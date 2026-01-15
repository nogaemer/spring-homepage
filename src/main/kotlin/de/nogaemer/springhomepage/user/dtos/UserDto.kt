package de.nogaemer.springhomepage.user.dtos

/**
 * Data Transfer Object for user information responses.
 *
 * Used to return user data to clients without exposing sensitive information.
 * Typically returned from authentication or user profile endpoints.
 *
 * Note: The `id` field uses Long type but users are stored with ObjectId in MongoDB.
 * Consider using ObjectId or String for consistency with User entity.
 *
 * @property id User's unique identifier
 * @property name User's display name
 * @property login User's login identifier (username or email)
 * @property token JWT authentication token for this user session
 */
data class UserDto(
    val id: Long,
    val name: String,
    val login: String,
    var token: String,
)