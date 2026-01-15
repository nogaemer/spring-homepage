package de.nogaemer.springhomepage.user.dtos

/**
 * Data Transfer Object for user information responses.
 *
 * Used to return user data to clients without exposing sensitive information.
 * Typically returned from authentication or user profile endpoints.
 *
 * **Known Issue**: The `id` field uses Long type but users are stored with ObjectId in MongoDB.
 * This type mismatch can cause mapping issues and should be addressed by:
 * - Changing id to ObjectId or String type for consistency with User entity
 * - Or implementing proper conversion logic between Long and ObjectId
 *
 * @property id User's unique identifier (Long - inconsistent with User entity's ObjectId)
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