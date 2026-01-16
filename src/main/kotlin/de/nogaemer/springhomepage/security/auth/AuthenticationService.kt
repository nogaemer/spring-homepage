package de.nogaemer.springhomepage.security.auth

import de.nogaemer.springhomepage.exceptions.AuthorisationRequired
import de.nogaemer.springhomepage.exceptions.NotFoundException
import de.nogaemer.springhomepage.security.config.JwtService
import de.nogaemer.springhomepage.security.token.Token
import de.nogaemer.springhomepage.security.token.TokenRepository
import de.nogaemer.springhomepage.security.token.TokenType
import de.nogaemer.springhomepage.user.Role
import de.nogaemer.springhomepage.user.User
import de.nogaemer.springhomepage.user.UserRepository
import de.nogaemer.springhomepage.utils.EnvUtils
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.http.HttpHeaders
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.function.Consumer

/**
 * Service responsible for managing user authentication and registration flows.
 *
 * This service handles:
 * - User registration with password encoding and token generation
 * - Standard login authentication with credentials
 * - OAuth2 authentication for external identity providers (Google, GitHub, etc.)
 * - JWT access token and refresh token generation
 * - Token lifecycle management (storage, validation, revocation)
 * - Automatic cleanup of expired tokens
 *
 * ## Authentication Flow:
 * 1. User submits credentials (login/password) or OAuth2 authentication
 * 2. Credentials are validated via AuthenticationManager or OAuth2 provider
 * 3. JWT access token (short-lived) and refresh token (long-lived) are generated
 * 4. Tokens are stored in MongoDB with reference to user
 * 5. AuthenticationResponse containing both tokens is returned to client
 *
 * ## Token Lifecycle:
 * - Access tokens are used for API authentication (short TTL)
 * - Refresh tokens are used to obtain new access tokens (long TTL)
 * - Tokens are stored in database with revoked/expired flags
 * - Expired tokens are automatically cleaned up every hour
 * - On logout, tokens are marked as revoked and expired
 *
 * @property repository Repository for user CRUD operations
 * @property tokenRepository Repository for token storage and retrieval
 * @property passwordEncoder Encoder for hashing user passwords
 * @property jwtService Service for JWT token generation and validation
 * @property authenticationManager Spring Security authentication manager
 */
@Service
@RequiredArgsConstructor
class AuthenticationService(
    private val repository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager,
) {

    /**
     * Registers a new user and generates authentication tokens.
     *
     * ## Registration Flow:
     * 1. Validates that request is not null
     * 2. Creates new User entity with encoded password
     * 3. Saves user to MongoDB
     * 4. Generates JWT access token and refresh token
     * 5. Stores access token in database linked to user
     * 6. Returns AuthenticationResponse with tokens and user ID
     *
     * @param request Registration details including name, login, password, and role
     * @return AuthenticationResponse containing access token, refresh token, and user ID
     * @throws IllegalArgumentException if request is null
     */
    fun register(request: RegisterRequest?): AuthenticationResponse {
        request ?: throw IllegalArgumentException("Request must not be null")

        val user = User(
            request.name,
            request.login,
            passwordEncoder.encode(request.password),
            request.role
        )
        val savedUser = repository.save(user)
        val jwtToken = jwtService.generateToken(user)
        val refreshToken = jwtService.generateRefreshToken(user)
        saveUserToken(savedUser, jwtToken)
        return AuthenticationResponse(
            jwtToken,
            refreshToken,
            savedUser.id
        )
    }

    /**
     * Authenticates a user with username and password credentials.
     *
     * ## Login Flow:
     * 1. Validates request is not null
     * 2. Authenticates credentials via Spring Security AuthenticationManager
     * 3. Retrieves user from database by login
     * 4. Generates new JWT access token and refresh token
     * 5. Stores new access token in database
     * 6. Returns AuthenticationResponse with tokens and user ID
     *
     * Note: This does not revoke previous tokens. Multiple sessions can coexist.
     *
     * @param request Login credentials (username/email and password)
     * @return AuthenticationResponse containing access token, refresh token, and user ID
     * @throws IllegalArgumentException if request is null
     * @throws NotFoundException if user is not found in database
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid
     */
    fun authenticate(request: AuthenticationRequest?): AuthenticationResponse {
        request ?: throw IllegalArgumentException("Request must not be null")

        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                request.login,
                request.password
            )
        )
        val user: User? = repository.findByLogin(request.login)
            .orElseThrow { NotFoundException("User not found") }
        val jwtToken = jwtService.generateToken(user as UserDetails)
        val refreshToken = jwtService.generateRefreshToken(user as UserDetails)
        saveUserToken(user, jwtToken)
        return AuthenticationResponse(
            jwtToken,
            refreshToken,
            user.id
        )
    }

    /**
     * Authenticates a user via OAuth2 provider (Google, GitHub, etc.).
     *
     * ## OAuth2 Authentication Flow:
     * 1. Extracts email and name from OAuth2 provider user profile
     * 2. Looks up existing user by email/login
     * 3. If user doesn't exist and ALLOW_OAUTH2_USER_REGISTRATION=true:
     *    - Creates new user with empty password (OAuth2 users don't need password)
     *    - Assigns USER role by default
     * 4. Generates JWT access token and refresh token
     * 5. Stores token in database linked to user
     * 6. Returns AuthenticationResponse with tokens
     *
     * @param oauth2User OAuth2 user principal from authentication provider
     * @return AuthenticationResponse with tokens, or null if email/name missing or registration disabled
     */
    fun authenticateOAuth2User(oauth2User: OAuth2User): AuthenticationResponse? {

        // Extract user information from OAuth2 provider
        val email = extractEmail(oauth2User)
        val name = extractName(oauth2User)

        if (email != null && name != null) {
            // Find or create user
            val user = repository.findByLogin(email).orElseGet {
                if (!EnvUtils.getEnvVariable("ALLOW_OAUTH2_USER_REGISTRATION")
                        .equals("true", true)
                ) return@orElseGet null

                // Create new user if doesn't exist
                val newUser = User(
                    name,
                    email,
                    "", // No password for OAuth2 users
                    Role.USER
                )
                val savedUser = repository.save(newUser)
                return@orElseGet savedUser
            }

            val jwtToken = jwtService.generateToken(user as UserDetails)
            val refreshToken = jwtService.generateRefreshToken(user as UserDetails)
            saveUserToken(user, jwtToken)

            return AuthenticationResponse(
                jwtToken,
                refreshToken,
                user.id
            )
        }
        return null
    }

    /**
     * Extracts email from OAuth2 user attributes.
     *
     * Supports multiple OAuth2 providers:
     * - Standard "email" attribute (Google, most providers)
     * - GitHub's "login" attribute as fallback
     *
     * @param oauth2User OAuth2 user principal
     * @return Email address or null if not found
     */
    private fun extractEmail(oauth2User: OAuth2User): String? {
        return oauth2User.getAttribute<String>("email")
            ?: oauth2User.getAttribute<String>("login") // GitHub
    }

    /**
     * Extracts display name from OAuth2 user attributes.
     *
     * Supports multiple OAuth2 providers with fallback chain:
     * - "name" attribute (most providers)
     * - "login" attribute (GitHub)
     * - "given_name" attribute (Google)
     *
     * @param oauth2User OAuth2 user principal
     * @return Display name or null if not found
     */
    private fun extractName(oauth2User: OAuth2User): String? {
        return oauth2User.getAttribute<String>("name")
            ?: oauth2User.getAttribute<String>("login") // GitHub fallback
            ?: oauth2User.getAttribute<String>("given_name") // Google fallback
    }

    /**
     * Generates JWT token for an OAuth2 user.
     *
     * This is a legacy/alternative method for OAuth2 token generation.
     * Finds or creates user and generates JWT token.
     *
     * @param email User's email address
     * @param name User's display name
     * @return Generated JWT token string
     */
    fun generateTokenForOAuth2User(email: String, name: String): String {
        // Check if user exists in your database
        val user = repository.findByLogin(email).orElseGet {
            // Create new user if doesn't exist
            val newUser = User(
                name,
                email,
                passwordEncoder.encode(""),
                Role.USER
            )
            repository.save(newUser)
        }

        // Generate and return JWT token
        return jwtService.generateToken(user)
    }

    /**
     * Saves a JWT token to the database with collision detection.
     *
     * ## Token Storage Process:
     * 1. Checks if token with same value already exists in database
     * 2. If collision detected, regenerates token with new value
     * 3. Repeats until unique token is generated
     * 4. Creates Token entity with revoked=false, expired=false defaults
     * 5. Links token to user via @DBRef relationship
     * 6. Saves and returns the Token entity
     *
     * This prevents rare JWT collision scenarios where identical tokens
     * could be generated for different users.
     *
     * @param user User to associate with the token
     * @param jwtToken JWT token string to save
     * @return Saved Token entity
     */
    private fun saveUserToken(user: User, jwtToken: String): Token {
        var newJwtToken = jwtToken

        // Check if a token with the same value already exists
        var existingToken = tokenRepository.findByToken(jwtToken)
        while (existingToken != null) {
            // Token with the same value already exists, regenerate the token
            newJwtToken = jwtService.generateToken(user)
            existingToken = tokenRepository.findByToken(newJwtToken)
        }

        // Save the new token
        val token = Token(
            token = newJwtToken,
            tokenType = TokenType.BEARER,
            revoked = false,
            expired = false,
            user = user
        )
        return tokenRepository.save(token)
    }

    /**
     * Revokes all valid tokens for a user.
     *
     * Marks all non-expired, non-revoked tokens as both expired and revoked.
     * Used when forcing logout from all sessions or before issuing new tokens.
     *
     * @param user User whose tokens should be revoked
     */
    private fun revokeAllUserTokens(user: User) {
        val validUserTokens = tokenRepository.findAllValidTokenByUser(user.id!!)
        if (validUserTokens.isEmpty()) return
        validUserTokens.forEach(Consumer { token: Token ->
            token.expired = true
            token.revoked = true
        })
        tokenRepository.saveAll(validUserTokens)
    }

    /**
     * Refreshes an access token using a valid refresh token.
     *
     * ## Token Refresh Flow:
     * 1. Extracts Bearer token from Authorization header
     * 2. Validates refresh token exists in database
     * 3. Extracts username from refresh token JWT claims
     * 4. Validates refresh token is still valid and not expired
     * 5. Revokes the old access token (marks as expired/revoked)
     * 6. Generates new access token
     * 7. Saves new access token to database
     * 8. Returns AuthenticationResponse with new access token and same refresh token
     *
     * Note: Refresh token is reused, only access token is regenerated.
     *
     * @param request HTTP request containing Authorization header with refresh token
     * @return AuthenticationResponse with new access token and existing refresh token
     * @throws IllegalArgumentException if request is null
     * @throws AuthorisationRequired if Authorization header missing or token invalid
     * @throws NotFoundException if token or user not found in database
     * @throws IOException if I/O error occurs
     */
    @Throws(IOException::class)
    fun refreshToken(
        request: HttpServletRequest?
    ): AuthenticationResponse {
        request
            ?: throw IllegalArgumentException("Request must not be null")

        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        val userLogin: String

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw AuthorisationRequired("Authorization header is missing")
        }

        val refreshToken = authHeader.substring(7)
        tokenRepository.findByToken(refreshToken) ?: throw NotFoundException("Token not found")
        userLogin = jwtService.extractUsername(refreshToken)

        val user = repository.findByLogin(userLogin)
            .orElseThrow { NotFoundException("User not found") }

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw AuthorisationRequired("Token is not valid")
        }

        val oldAccessToken = tokenRepository.findTokenByUserAndToken(user, refreshToken)
        oldAccessToken?.let {
            it.revoked = true
            it.expired = true
            tokenRepository.save(it)
        }
        // Generate a new access token
        val newAccessToken = saveUserToken(user, jwtService.generateToken(user))
        val authResponse = AuthenticationResponse(
            newAccessToken.token,
            refreshToken,
            user.id
        )
        return authResponse
    }

    /**
     * Scheduled task to clean up expired tokens from the database.
     *
     * Runs every hour (3600000 ms) to:
     * 1. Fetch all tokens from database
     * 2. Check each token's expiration status via JWT validation
     * 3. Delete tokens that are expired or marked as expired
     *
     * This prevents token table bloat and removes tokens that can no longer be used.
     * Helps maintain database performance by removing stale authentication data.
     */
    @Scheduled(fixedRate = 3600000) // runs every hour
    fun removeExpiredTokens() {
        val allTokens = tokenRepository.findAll()
        val expiredTokens = allTokens.filter {
            try {
                jwtService.isTokenExpired(it.token)
                it.expired
            } catch (_: AuthorisationRequired) {
                true
            }
        }
        tokenRepository.deleteAll(expiredTokens)
    }
}

/**
 * Success handler for OAuth2 authentication flows.
 *
 * Handles the redirect after successful OAuth2 authentication with external providers
 * (Google, GitHub, etc.). Generates JWT tokens and redirects user to frontend callback URL.
 *
 * ## OAuth2 Flow:
 * 1. User authenticates with OAuth2 provider (Google/GitHub/etc.)
 * 2. Provider redirects back to application with OAuth2 user data
 * 3. This handler is invoked by Spring Security
 * 4. Generates JWT access and refresh tokens via AuthenticationService
 * 5. Redirects to frontend callback URL with tokens in query parameters
 * 6. Frontend extracts tokens and stores them for subsequent API calls
 *
 * @property authenticationService Service for generating JWT tokens
 */
@Component
class OAuth2AuthenticationSuccessHandler(
    private val authenticationService: AuthenticationService,
) : AuthenticationSuccessHandler {
    val baseUrl = EnvUtils.getEnvVariable("CLIENT_BASE_URL")
        ?: throw IllegalStateException("CLIENT_BASE_URL environment variable is not set")

    /**
     * Handles successful OAuth2 authentication and redirects with tokens.
     *
     * Called automatically by Spring Security after OAuth2 provider authentication succeeds.
     *
     * Success case: Redirects to `{CLIENT_BASE_URL}/auth/callback?type=success&token=...&refreshToken=...&userId=...`
     * Error cases:
     * - User not found/registration disabled: `{CLIENT_BASE_URL}/auth/callback?type=error&message=oauth2_user_not_found`
     * - Token generation failed: `{CLIENT_BASE_URL}/auth/callback?type=error&message=token_generation_failed`
     *
     * @param request HTTP request
     * @param response HTTP response for sending redirect
     * @param authentication Spring Security authentication containing OAuth2User principal
     */
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        try {
            val authenticationResponse =
                authenticationService.authenticateOAuth2User(authentication.principal as OAuth2User)

            if (authenticationResponse == null) {
                response.sendRedirect("${baseUrl}/auth/callback?type=error&message=oauth2_user_not_found")
                return
            }

            response.sendRedirect(
                "${baseUrl}/auth/callback?type=success" +
                        "&token=${authenticationResponse.accessToken}" +
                        "&refreshToken=${authenticationResponse.refreshToken}" +
                        "&userId=${authenticationResponse.userId}"
            )
        } catch (_: Exception) {
            // Handle token generation error
            response.sendRedirect("${baseUrl}/auth/callback?type=error&message=token_generation_failed")
        }
    }
}