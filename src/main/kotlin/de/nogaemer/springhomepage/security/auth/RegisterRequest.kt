package de.nogaemer.springhomepage.security.auth

import de.nogaemer.springhomepage.user.Role
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor

/**
 * Request DTO for user registration.
 *
 * Contains all required information to create a new user account.
 * Submitted to `/api/v1/auth/register` endpoint (admin only).
 *
 * @property name User's display name
 * @property login User's unique login identifier (username or email)
 * @property password Plaintext password (will be encoded before storage)
 * @property role User's role determining permissions (USER, MANAGER, or ADMIN)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
data class RegisterRequest(
    val name: String,
    val login: String,
    val password: String,
    val role: Role
)
