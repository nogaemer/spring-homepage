/*
 * JWT (JSON Web Token) service for token generation, validation, and management.
 *
 * This service handles all JWT operations including:
 * - Token generation (access and refresh tokens)
 * - Token parsing and validation
 * - Claims extraction (username, expiration, custom claims)
 * - Token expiration checking
 * - User extraction from HTTP requests
 * - Expired token cleanup from database
 *
 * JWT Structure:
 * - Header: Algorithm (HS256) and token type
 * - Payload: Claims including subject (username), issued at, expiration, and custom claims
 * - Signature: HMAC SHA-256 signature using secret key
 *
 * Token Types:
 * - Access Token: Short-lived token for API authentication (configured via jwt.expiration)
 * - Refresh Token: Long-lived token for obtaining new access tokens (configured via jwt.refresh-token.expiration)
 *
 * Security Notes:
 * - Tokens are signed with HS256 algorithm using a Base64-encoded secret key
 * - Expired tokens are automatically deleted from the database during validation
 * - Token validation includes signature verification, expiration check, and database lookup
 *
 * @author Spring Homepage Security Team
 * @since 1.0
 */
package de.nogaemer.springhomepage.security.config

import de.nogaemer.springhomepage.exceptions.AuthorisationRequired
import de.nogaemer.springhomepage.exceptions.NotFoundException
import de.nogaemer.springhomepage.security.token.TokenRepository
import de.nogaemer.springhomepage.user.User
import de.nogaemer.springhomepage.user.UserRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.security.Key
import java.util.*
import java.util.function.Function

/**
 * Service for JWT token operations including generation, validation, and claims extraction.
 *
 * Handles the complete JWT lifecycle:
 * - Creating signed tokens with configurable expiration times
 * - Extracting and validating user information from tokens
 * - Managing token expiration and revocation
 * - Cleaning up expired tokens from persistent storage
 */
@Service
class JwtService {
    /** Base64-encoded secret key for signing JWT tokens */
    @Value("\${application.security.jwt.secret-key}")
    private val secretKey: String? = null

    /** Access token expiration time in milliseconds */
    @Value("\${application.security.jwt.expiration}")
    private val jwtExpiration: Long = 0

    /** Refresh token expiration time in milliseconds */
    @Value("\${application.security.jwt.refresh-token.expiration}")
    private val refreshExpiration: Long = 0

    @Autowired
    private val userRepository: UserRepository? = null

    @Autowired
    private val tokenRepository: TokenRepository? = null

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * The username is stored in the token's subject claim and is used to identify
     * the authenticated user throughout the application.
     *
     * @param token JWT token string
     * @return Username extracted from the token's subject claim
     */
    fun extractUsername(token: String?): String {
        return extractClaim(token) { obj: Claims -> obj.subject }
    }

    /**
     * Extracts the authenticated user from an HTTP request's Authorization header.
     *
     * This method:
     * 1. Retrieves the Bearer token from the Authorization header
     * 2. Extracts the username from the token
     * 3. Looks up and returns the User entity from the database
     *
     * Useful for endpoints that need to access the current user without using Spring Security's
     * SecurityContext (e.g., in WebSocket handlers or custom filters).
     *
     * @param request HTTP servlet request containing the Authorization header
     * @return User entity if found, null if token is missing or user doesn't exist
     */
    fun extractUserFromRequest(request: HttpServletRequest): User? {
        val token = request.getHeader("Authorization")?.removePrefix("Bearer ")
        val username = token?.let { extractUsername(it) }
        return username?.let { userRepository!!.findByLogin(it).orElse(null) }
    }

    /**
     * Extracts a specific claim from a JWT token.
     *
     * Generic method for retrieving any claim from the token's payload using a custom resolver function.
     * Common claims include subject (username), expiration date, issued at date, and custom claims.
     *
     * @param T The type of the claim value
     * @param token JWT token string
     * @param claimsResolver Function to extract the desired claim from the Claims object
     * @return The extracted claim value
     */
    fun <T> extractClaim(token: String?, claimsResolver: Function<Claims, T>): T {
        val claims = extractAllClaims(token)
        return claimsResolver.apply(claims)
    }

    /**
     * Generates a standard access token for the given user.
     *
     * Creates a JWT token with default claims (subject, issued at, expiration) and the
     * configured access token expiration time. This is the primary authentication token
     * used for API requests.
     *
     * @param userDetails User details containing username and authorities
     * @return Signed JWT access token string
     */
    fun generateToken(userDetails: UserDetails): String {
        return generateToken(HashMap(), userDetails)
    }

    /**
     * Generates an access token with custom additional claims.
     *
     * Allows including extra information in the token payload beyond the standard claims.
     * Custom claims might include user roles, permissions, or other metadata needed for
     * authorization decisions without additional database lookups.
     *
     * @param extraClaims Map of additional claims to include in the token
     * @param userDetails User details containing username and authorities
     * @return Signed JWT access token string with custom claims
     */
    fun generateToken(
        extraClaims: Map<String?, Any?>,
        userDetails: UserDetails
    ): String {
        return buildToken(extraClaims, userDetails, jwtExpiration)
    }

    /**
     * Generates a refresh token for the given user.
     *
     * Refresh tokens have a longer expiration time than access tokens and are used to obtain
     * new access tokens without requiring the user to re-authenticate. This implements the
     * refresh token pattern for improved security and user experience.
     *
     * Token Lifecycle:
     * 1. User logs in and receives both access and refresh tokens
     * 2. Access token is used for API requests until it expires
     * 3. When access token expires, refresh token is used to obtain a new access token
     * 4. Refresh token is only valid if not revoked in the database
     *
     * @param userDetails User details containing username and authorities
     * @return Signed JWT refresh token string with extended expiration
     */
    fun generateRefreshToken(
        userDetails: UserDetails
    ): String {
        return buildToken(HashMap(), userDetails, refreshExpiration)
    }

    /**
     * Builds a JWT token with specified claims, user details, and expiration time.
     *
     * This internal method constructs the complete JWT token:
     * 1. Sets all custom claims from the extraClaims map
     * 2. Sets the subject (username) from userDetails
     * 3. Sets the issued at timestamp to current time
     * 4. Sets the expiration timestamp based on the provided expiration duration
     * 5. Signs the token using HS256 algorithm with the secret key
     * 6. Compacts the token into its final string representation
     *
     * @param extraClaims Map of additional claims to include in the token payload
     * @param userDetails User details containing username
     * @param expiration Token expiration duration in milliseconds
     * @return Compact JWT string representation
     */
    private fun buildToken(
        extraClaims: Map<String?, Any?>,
        userDetails: UserDetails,
        expiration: Long
    ): String {
        return Jwts
            .builder()
            .setClaims(extraClaims)
            .setSubject(userDetails.username)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .signWith(signInKey, SignatureAlgorithm.HS256)
            .compact()
    }

    /**
     * Validates a JWT token against the given user details.
     *
     * Performs two validation checks:
     * 1. Username match: Verifies the token's subject matches the provided username
     * 2. Expiration check: Ensures the token has not expired
     *
     * Note: Token signature is implicitly validated when claims are extracted.
     * Database token status (revoked/expired) is checked separately in the authentication filter.
     *
     * @param token JWT token string to validate
     * @param userDetails User details to validate against
     * @return true if token is valid and not expired, false otherwise
     */
    fun isTokenValid(token: String?, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return (username == userDetails.username) && !isTokenExpired(token)
    }

    /**
     * Checks if a JWT token has expired.
     *
     * Compares the token's expiration date with the current date/time.
     * Expired tokens should not be accepted for authentication.
     *
     * @param token JWT token string to check
     * @return true if the token has expired, false otherwise
     */
    fun isTokenExpired(token: String?): Boolean {
        return extractExpiration(token).before(Date())
    }

    /**
     * Extracts the expiration date from a JWT token.
     *
     * @param token JWT token string
     * @return Date object representing when the token expires
     */
    private fun extractExpiration(token: String?): Date {
        return extractClaim(token) { obj: Claims -> obj.expiration }
    }

    /**
     * Extracts all claims from a JWT token with validation and error handling.
     *
     * This method:
     * 1. Parses the JWT token using the signing key
     * 2. Validates the token signature
     * 3. Checks token expiration
     * 4. Returns the claims payload
     *
     * Error Handling:
     * - ExpiredJwtException: Token has expired
     *   - Automatically deletes the expired token from the database (cleanup)
     *   - Throws AuthorisationRequired exception
     * - Other exceptions (malformed token, invalid signature, etc.):
     *   - Logs the error
     *   - Throws AuthorisationRequired exception
     *
     * Note: This method performs a database operation when a token is expired,
     * which is necessary for immediate token revocation but adds a database call
     * to the validation path. This is an intentional trade-off for security.
     *
     * @param token JWT token string to parse
     * @return Claims object containing all token claims
     * @throws AuthorisationRequired if token is expired, invalid, or malformed
     * @throws NotFoundException if expired token is not found in database during cleanup
     */
    fun extractAllClaims(token: String?): Claims {
        try {
            return Jwts
                .parserBuilder()
                .setSigningKey(signInKey)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (_: ExpiredJwtException) {
            // Automatic cleanup: Delete expired token from database to prevent reuse
            tokenRepository!!.delete(tokenRepository.findByToken(token!!) ?:
            throw NotFoundException("Token not found"))

            throw AuthorisationRequired("Token is expired")
        } catch (e: Exception) {
            println("Failed to parse token: $e")
            throw AuthorisationRequired("Token is invalid")
        }
    }

    /**
     * Provides the signing key for JWT token operations.
     *
     * The signing key is used to:
     * - Sign tokens during generation (ensures authenticity)
     * - Verify token signatures during validation (prevents tampering)
     *
     * The secret key is:
     * 1. Loaded from application configuration (application.security.jwt.secret-key)
     * 2. Base64-decoded to obtain the raw bytes
     * 3. Converted to an HMAC SHA key for the HS256 algorithm
     *
     * Security Note: The secret key should be:
     * - At least 256 bits (32 bytes) for HS256
     * - Stored securely (environment variables, secrets manager)
     * - Never committed to version control
     * - Rotated periodically in production
     *
     * @return HMAC SHA Key for signing and verifying JWT tokens
     */
    private val signInKey: Key
        get() {
            val keyBytes = Decoders.BASE64.decode(secretKey)
            return Keys.hmacShaKeyFor(keyBytes)
        }
}
