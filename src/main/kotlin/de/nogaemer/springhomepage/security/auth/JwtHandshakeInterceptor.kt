package de.nogaemer.springhomepage.security.auth

import de.nogaemer.springhomepage.security.config.JwtService
import de.nogaemer.springhomepage.security.token.TokenRepository
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

/**
 * WebSocket handshake interceptor for JWT-based authentication.
 *
 * Intercepts WebSocket connection attempts to validate JWT tokens before
 * establishing the WebSocket session. Tokens are passed as query parameters
 * since WebSocket handshake doesn't support custom headers in all browsers.
 *
 * ## Authentication Flow
 * 1. Client connects to WebSocket endpoint with token query parameter
 * 2. Interceptor extracts token from query string
 * 3. Validates token signature and expiration via [JwtService]
 * 4. Checks token exists in database and is not revoked/expired
 * 5. Verifies token's user matches the claimed user
 * 6. Creates authentication object and stores in session attributes
 * 7. Allows/rejects connection based on validation
 *
 * ## Token Validation
 * A token is considered valid when:
 * - Token signature is valid (signed by server secret)
 * - Token is not expired (expiration timestamp check)
 * - Token exists in database (not deleted)
 * - Token is not marked as revoked
 * - Token is not marked as expired in database
 * - Token's associated user matches the JWT subject (email)
 *
 * ## Connection URL Format
 * ```
 * ws://localhost:8080/ws/endpoint?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * ```
 *
 * ## Security Considerations
 * - Token in query string is visible in logs and URLs (consider alternatives for production)
 * - Token replay attacks possible if stolen (use short expiration times)
 * - No support for token refresh during active WebSocket session
 *
 * ## Integration Points
 * - Used by WebSocket configuration in [de.nogaemer.springhomepage.security.config.WebSocketConfig]
 * - Works with [AuthHandshakeHandler] to establish authenticated Principal
 * - Relies on [JwtService] for token validation
 * - Queries [TokenRepository] for token status
 *
 * @property jwtService Service for JWT token parsing and validation
 * @property tokenRepository Repository for querying stored token status
 *
 * @see AuthHandshakeHandler
 * @see de.nogaemer.springhomepage.security.config.WebSocketConfig
 * @see de.nogaemer.springhomepage.security.config.JwtService
 */
@Component
class JwtHandshakeInterceptor(
    private val jwtService: JwtService,
    private val tokenRepository: TokenRepository
) : HandshakeInterceptor {

    /**
     * Validates JWT token before allowing WebSocket handshake to proceed.
     *
     * Called before the WebSocket connection is established. Extracts and validates
     * JWT token from query parameters, storing authentication in session attributes
     * if valid.
     *
     * ## Process
     * 1. Cast request to ServletServerHttpRequest to access query parameters
     * 2. Extract "token" query parameter
     * 3. Parse JWT claims using JwtService
     * 4. Lookup token in database via TokenRepository
     * 5. Validate token is active (not expired/revoked)
     * 6. Verify token's user matches JWT subject
     * 7. Create authentication object and store in attributes
     * 8. Return true to allow connection, false to reject
     *
     * ## Validation Checks
     * - Token parameter exists
     * - Token signature is valid
     * - Token exists in database
     * - Token not expired (database flag)
     * - Token not revoked (database flag)
     * - Token's user email matches JWT subject
     *
     * ## Session Attributes
     * On successful validation, stores authentication as "user" attribute:
     * ```kotlin
     * attributes["user"] = UsernamePasswordAuthenticationToken(user, null, authorities)
     * ```
     * This is retrieved by [AuthHandshakeHandler.determineUser].
     *
     * ## Error Handling
     * Catches all exceptions during validation and returns false,
     * preventing connection with invalid/malformed tokens.
     *
     * @param request WebSocket handshake request (contains query parameters)
     * @param response WebSocket handshake response (not used)
     * @param wsHandler WebSocket handler for the endpoint (not used)
     * @param attributes Mutable map for storing session attributes (authentication stored here)
     * @return true to allow connection, false to reject
     */
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        if (request is ServletServerHttpRequest) {
            // Extract token from Query Parameter "token"
            val token = request.servletRequest.getParameter("token")

            if (token != null) {
                try {
                    // Validate token logic (Reuse your logic from JwtAuthenticationFilter)
                    val claims = jwtService.extractAllClaims(token)
                    val userEmail = claims.subject
                    val storedToken = tokenRepository.findByToken(token)

                    if (storedToken != null && !storedToken.expired && !storedToken.revoked && storedToken.user!!.username == userEmail) {
                        // Valid! Set the user for this WebSocket session
                        val user = storedToken.user
                        val auth = UsernamePasswordAuthenticationToken(user, null, user!!.authorities)

                        // Store auth in attributes (Spring Security uses this for the WS session)
                        attributes["user"] = auth
                        return true
                    }
                } catch (_: Exception) {
                    // Invalid token
                    return false
                }
            }
        }
        return false // No token or invalid -> Reject connection
    }

    /**
     * Called after successful WebSocket handshake completion.
     *
     * This implementation is empty as all processing is done in [beforeHandshake].
     * Provided for interface compliance.
     *
     * ## Use Cases
     * Could be used for:
     * - Logging successful connections
     * - Post-connection setup
     * - Metrics collection
     *
     * @param request WebSocket handshake request
     * @param response WebSocket handshake response
     * @param wsHandler WebSocket handler for the endpoint
     * @param exception Exception that occurred during handshake, or null if successful
     */
    override fun afterHandshake(request: ServerHttpRequest, response: ServerHttpResponse, wsHandler: WebSocketHandler, exception: Exception?) {}
}

