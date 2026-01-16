package de.nogaemer.springhomepage.security.config

import de.nogaemer.springhomepage.security.token.Token
import de.nogaemer.springhomepage.security.token.TokenRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.stereotype.Service

/**
 * Service that handles user logout and token revocation.
 *
 * Implements Spring Security's LogoutHandler to perform custom logout logic.
 * When a user logs out, this service:
 * 1. Extracts the JWT token from the Authorization header
 * 2. Marks the token as expired and revoked in the database
 * 3. Clears the Spring Security context
 *
 * ## Token Revocation:
 * Tokens are not deleted but marked with `expired=true` and `revoked=true`.
 * This allows audit trails and prevents reuse of logged-out tokens.
 * The JwtAuthenticationFilter will reject any revoked/expired tokens.
 *
 * ## Logout Flow:
 * 1. Client sends logout request with `Authorization: Bearer <token>` header
 * 2. This handler extracts token from header
 * 3. Looks up token in database
 * 4. Marks token as expired and revoked
 * 5. Clears SecurityContext to invalidate session
 *
 * Note: This only revokes the specific token used for logout.
 * Other active tokens for the same user remain valid unless explicitly revoked.
 *
 * @property tokenRepository Repository for token storage and retrieval
 */
@Service
@RequiredArgsConstructor
class LogoutService : LogoutHandler {

    @Autowired
    private val tokenRepository: TokenRepository? = null

    /**
     * Performs logout by revoking the JWT token and clearing security context.
     *
     * Called automatically by Spring Security during logout process.
     *
     * If Authorization header is missing or token not found in database,
     * the method returns silently without error (graceful degradation).
     *
     * @param request HTTP request containing Authorization header with token
     * @param response HTTP response (unused)
     * @param authentication Spring Security authentication object (unused)
     */
    override fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return
        }
        val jwt = authHeader.substring(7)
        val storedToken = tokenRepository!!.findByToken(jwt)
            ?: return

        storedToken.expired = true
        storedToken.revoked = true
        tokenRepository.save<Token>(storedToken)
        SecurityContextHolder.clearContext()
    }
}
