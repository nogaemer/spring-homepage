package de.nogaemer.springhomepage.security.auth

import org.springframework.http.server.ServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal

/**
 * Custom WebSocket handshake handler for establishing authenticated Principal.
 *
 * Extends Spring's DefaultHandshakeHandler to provide custom user determination
 * logic for WebSocket sessions. Retrieves the authenticated user from session
 * attributes populated by [JwtHandshakeInterceptor].
 *
 * ## Purpose
 * In WebSocket connections, the Principal determines the user identity for:
 * - User-specific message routing (via @MessageMapping with Principal parameter)
 * - Authorization decisions
 * - Session management
 *
 * ## Integration Flow
 * 1. [JwtHandshakeInterceptor] validates JWT and stores authentication in attributes
 * 2. This handler retrieves authentication from attributes
 * 3. Authentication becomes the Principal for the WebSocket session
 * 4. Controllers can access Principal via method parameters
 *
 * ## Usage in Controllers
 * ```kotlin
 * @MessageMapping("/message")
 * fun handleMessage(principal: Principal, message: String) {
 *     // principal.name contains the user's email
 * }
 * ```
 *
 * ## Configuration
 * Registered in WebSocket configuration:
 * ```kotlin
 * override fun registerStompEndpoints(registry: StompEndpointRegistry) {
 *     registry.addEndpoint("/ws")
 *         .setHandshakeHandler(AuthHandshakeHandler())
 *         .addInterceptors(jwtHandshakeInterceptor)
 * }
 * ```
 *
 * @see JwtHandshakeInterceptor
 * @see de.nogaemer.springhomepage.security.config.WebSocketConfig
 * @see org.springframework.web.socket.server.support.DefaultHandshakeHandler
 */
class AuthHandshakeHandler : DefaultHandshakeHandler() {
    /**
     * Determines the user Principal for the WebSocket session.
     *
     * Retrieves the authentication object stored by [JwtHandshakeInterceptor]
     * from session attributes and returns it as the Principal.
     *
     * ## Process
     * 1. Look up "user" key in attributes map
     * 2. Cast to Principal (safe because JwtHandshakeInterceptor stores UsernamePasswordAuthenticationToken)
     * 3. Return Principal or null if not found
     *
     * ## Null Handling
     * Returns null if:
     * - Authentication not found in attributes (interceptor rejected connection)
     * - Attribute exists but is not a Principal (should never happen)
     *
     * ## Principal Contents
     * The returned Principal (UsernamePasswordAuthenticationToken) contains:
     * - **principal**: User object with all details
     * - **credentials**: null (not needed after authentication)
     * - **authorities**: User's granted authorities/roles
     *
     * @param req Handshake request
     * @param wsHandler WebSocket handler for the endpoint
     * @param attributes Session attributes populated during handshake
     * @return Principal for the WebSocket session, or null if authentication not available
     */
    override fun determineUser(req: ServerHttpRequest, wsHandler: WebSocketHandler, attributes: MutableMap<String, Any>): Principal? {
        return attributes["user"] as? Principal
    }
}
