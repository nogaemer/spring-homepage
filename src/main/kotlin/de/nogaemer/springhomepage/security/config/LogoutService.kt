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

@Service
@RequiredArgsConstructor
class LogoutService : LogoutHandler {

    @Autowired
    private val tokenRepository: TokenRepository? = null

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
