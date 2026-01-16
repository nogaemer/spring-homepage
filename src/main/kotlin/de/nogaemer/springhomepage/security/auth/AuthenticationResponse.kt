package de.nogaemer.springhomepage.security.auth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId

/**
 * Response DTO containing authentication tokens and user information.
 *
 * Returned by authentication endpoints (register, authenticate, refreshToken)
 * after successful authentication or registration.
 *
 * ## Token Usage:
 * - **Access Token**: Short-lived JWT token for API authentication. Include in Authorization header
 *   for protected endpoints: `Authorization: Bearer <accessToken>`
 * - **Refresh Token**: Long-lived token used to obtain new access tokens when they expire.
 *   Send to `/api/v1/auth/refresh-token` endpoint to get new access token.
 * - **User ID**: MongoDB ObjectId of the authenticated user
 *
 * @property accessToken JWT access token for API authentication (short TTL, typically 15-60 minutes)
 * @property refreshToken JWT refresh token for obtaining new access tokens (long TTL, typically days/weeks)
 * @property userId MongoDB ObjectId of the authenticated user (serialized as string in JSON)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
data class AuthenticationResponse(
    val accessToken: String? = null,

    val refreshToken: String? = null,

    @field:JsonSerialize(using = ToStringSerializer::class)
    val userId: ObjectId? = null
)
