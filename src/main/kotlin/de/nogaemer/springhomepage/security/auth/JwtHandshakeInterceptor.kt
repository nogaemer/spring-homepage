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

@Component
class JwtHandshakeInterceptor(
    private val jwtService: JwtService,
    private val tokenRepository: TokenRepository
) : HandshakeInterceptor {

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

    override fun afterHandshake(request: ServerHttpRequest, response: ServerHttpResponse, wsHandler: WebSocketHandler, exception: Exception?) {}
}

