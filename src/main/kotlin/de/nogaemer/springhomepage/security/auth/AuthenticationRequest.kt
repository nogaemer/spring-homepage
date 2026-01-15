package de.nogaemer.springhomepage.security.auth

import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor

/**
 * Request DTO for user authentication (login).
 *
 * Contains credentials required for username/password authentication flow.
 * Submitted to `/api/v1/auth/authenticate` endpoint.
 *
 * @property login User's login identifier (username or email)
 * @property password User's plaintext password (will be validated against encoded password in database)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class AuthenticationRequest {
    var login: String? = null
    var password: String? = null
}
