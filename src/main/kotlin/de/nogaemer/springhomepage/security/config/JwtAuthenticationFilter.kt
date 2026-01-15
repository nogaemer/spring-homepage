package de.nogaemer.springhomepage.security.config

import de.nogaemer.springhomepage.exceptions.AuthorisationRequired
import de.nogaemer.springhomepage.exceptions.NotFoundException
import de.nogaemer.springhomepage.security.token.TokenRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.lang.NonNull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
@RequiredArgsConstructor
class JwtAuthenticationFilter(
    val jwtService: JwtService,
    val tokenRepository: TokenRepository
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath ?: ""
        // Allow preflight OPTIONS
        if (request.method.equals("OPTIONS", ignoreCase = true)) return true
        // public auth endpoints
        if (path.startsWith("/api/v1/auth")) return true
        // common public/static/documentation paths
        if (path == "/" || path == "" || path == "/index.html") return true
        if (path.startsWith("/swagger") || path.startsWith("/webjars") || path.startsWith("/configuration")) return true
        if (path.startsWith("/ws-search")) return true
        return false
    }

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        @NonNull request: HttpServletRequest,
        @NonNull response: HttpServletResponse,
        @NonNull filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write("Authorization header is missing or invalid")
            return
        }
        val jwt = authHeader.substring(7)
        try {
            // Optimization: Parse token once to get claims and validate signature/expiration
            val claims = jwtService.extractAllClaims(jwt)
            val userEmail = claims.subject

            if (SecurityContextHolder.getContext().authentication == null) {
                // Optimization: Fetch token first to avoid double user fetch (once by username, once by DBRef in token)
                val storedToken = tokenRepository.findByToken(jwt)

                if (storedToken != null && !storedToken.expired && !storedToken.revoked) {
                    val user = storedToken.user
                    // Verify the token belongs to the user
                    // Note: Signature and expiration are already validated by extractAllClaims
                    if (user.username == userEmail) {
                        val authToken = UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.authorities
                        )
                        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authToken
                    } else {
                        response.status = HttpServletResponse.SC_FORBIDDEN
                        response.writer.write("Token is not valid or expired")
                        return
                    }
                } else {
                    response.status = HttpServletResponse.SC_FORBIDDEN
                    response.writer.write("Token is not valid or expired")
                    return
                }
            }
            filterChain.doFilter(request, response)
        } catch (_: AuthorisationRequired) {
            response.status = HttpServletResponse.SC_FORBIDDEN
            response.writer.write("Token is not valid or expired")
        } catch (_: NotFoundException) {
            response.status = HttpServletResponse.SC_FORBIDDEN
            response.writer.write("Token not found")
        } catch (e: Exception) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write("Unauthorized: ${e.message}")
        }
    }
}