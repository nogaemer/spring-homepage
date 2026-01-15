package de.nogaemer.springhomepage.security.auth

import de.nogaemer.springhomepage.exceptions.NotFoundException
import de.nogaemer.springhomepage.security.config.JwtService
import de.nogaemer.springhomepage.user.User
import de.nogaemer.springhomepage.user.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException

/**
 * REST controller for authentication endpoints.
 *
 * Provides HTTP endpoints for:
 * - User registration (admin only)
 * - User authentication (login)
 * - Token refresh
 * - OAuth2 login redirect
 *
 * All endpoints are under `/api/v1/auth` base path.
 *
 * @property repository User repository for database operations
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class AuthenticationController(
    private val repository: UserRepository,
) {

    @Autowired
    private val service: AuthenticationService? = null

    @Autowired
    private val jwtTokenService: JwtService? = null

    /**
     * Registers a new user in the system.
     *
     * Endpoint: `POST /api/v1/auth/register`
     *
     * **Security**: Requires admin:read authority (admin role only)
     *
     * Creates a new user account with encoded password and generates authentication tokens.
     *
     * @param request Registration details (name, login, password, role)
     * @return ResponseEntity containing AuthenticationResponse with access token, refresh token, and user ID
     */
    @PostMapping("/register")
    @PreAuthorize("hasAuthority('admin:read')")
    fun register(
        @RequestBody request: RegisterRequest?
    ): ResponseEntity<AuthenticationResponse> {
        return ResponseEntity.ok(service!!.register(request))
    }

    /**
     * Authenticates a user with username and password.
     *
     * Endpoint: `POST /api/v1/auth/authenticate`
     *
     * **Security**: Public endpoint (no authentication required)
     *
     * Validates credentials and returns JWT tokens for subsequent API access.
     *
     * @param request Authentication credentials (login and password)
     * @return ResponseEntity containing AuthenticationResponse with access token, refresh token, and user ID
     */
    @PostMapping("/authenticate")
    fun authenticate(
        @RequestBody request: AuthenticationRequest?
    ): ResponseEntity<AuthenticationResponse> {
        return ResponseEntity.ok(service!!.authenticate(request))
    }

    /**
     * Refreshes an expired access token using a refresh token.
     *
     * Endpoint: `GET /api/v1/auth/refresh-token`
     *
     * **Security**: Requires valid refresh token in Authorization header
     *
     * Generates a new access token while keeping the same refresh token.
     * Client should send: `Authorization: Bearer <refresh_token>`
     *
     * @param request HTTP request containing Authorization header with refresh token
     * @return ResponseEntity containing AuthenticationResponse with new access token
     * @throws IOException if I/O error occurs during token refresh
     */
    @GetMapping("/refresh-token")
    @Throws(IOException::class)
    fun refreshToken(
        request: HttpServletRequest?
    ): ResponseEntity<AuthenticationResponse> {
        return ResponseEntity.ok(service!!.refreshToken(request))
    }

    /**
     * Initiates OAuth2 login flow with external provider.
     *
     * Endpoint: `GET /api/v1/auth/login/{provider}`
     *
     * **Security**: Public endpoint
     *
     * Redirects to OAuth2 authorization endpoint for specified provider (e.g., google, github).
     * After successful authentication, user is redirected back to application with tokens.
     *
     * @param provider OAuth2 provider name (e.g., "google", "github")
     * @param response HTTP response for sending redirect
     */
    @GetMapping("/login/{provider}")
    fun login(@PathVariable provider: String, response: HttpServletResponse) {
        // This will redirect to OAuth2 provider
        response.sendRedirect("/oauth2/authorization/$provider")
    }

}
