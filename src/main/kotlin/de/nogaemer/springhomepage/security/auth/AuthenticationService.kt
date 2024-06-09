package de.nogaemer.springhomepage.security.auth

import com.fasterxml.jackson.databind.ObjectMapper
import de.nogaemer.springhomepage.exceptions.AuthorisationRequired
import de.nogaemer.springhomepage.exceptions.NotFoundException
import de.nogaemer.springhomepage.security.config.JwtService
import de.nogaemer.springhomepage.security.token.Token
import de.nogaemer.springhomepage.security.token.TokenRepository
import de.nogaemer.springhomepage.security.token.TokenType
import de.nogaemer.springhomepage.user.User
import de.nogaemer.springhomepage.user.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.function.Consumer

@Service
@RequiredArgsConstructor
class AuthenticationService {

    @Autowired
    private val repository: UserRepository? = null

    @Autowired
    private val tokenRepository: TokenRepository? = null

    @Autowired
    private val passwordEncoder: PasswordEncoder? = null

    @Autowired
    private val jwtService: JwtService? = null

    @Autowired
    private val authenticationManager: AuthenticationManager? = null

    fun register(request: RegisterRequest?): AuthenticationResponse {
        request ?: throw IllegalArgumentException("Request must not be null")

        val user = User(
            request.name,
            request.login,
            passwordEncoder!!.encode(request.password),
            request.role
        )
        val savedUser = repository!!.save(user)
        val jwtToken = jwtService!!.generateToken(user)
        val refreshToken = jwtService.generateRefreshToken(user)
        saveUserToken(savedUser, jwtToken)
        return AuthenticationResponse(
            jwtToken,
            refreshToken
        )
    }

    fun authenticate(request: AuthenticationRequest?): AuthenticationResponse {
        request ?: throw IllegalArgumentException("Request must not be null")

        authenticationManager!!.authenticate(
            UsernamePasswordAuthenticationToken(
                request.login,
                request.password
            )
        )
        val user: User? = repository!!.findByLogin(request.login)
            .orElseThrow()
        val jwtToken = jwtService!!.generateToken(user as UserDetails)
        val refreshToken = jwtService.generateRefreshToken(user as UserDetails)
        saveUserToken(user, jwtToken)
        return AuthenticationResponse(
            jwtToken,
            refreshToken
        )
    }

    private fun saveUserToken(user: User, jwtToken: String): Token {
        val token = Token(
            token = jwtToken,
            tokenType = TokenType.BEARER,
            revoked = false,
            expired = false,
            user = user
        )
        return tokenRepository!!.save(token)
    }

    private fun revokeAllUserTokens(user: User) {
        val validUserTokens = tokenRepository!!.findAllValidTokenByUser(user.id!!)
        if (validUserTokens.isEmpty()) return
        validUserTokens.forEach(Consumer { token: Token ->
            token.expired = true
            token.revoked = true
        })
        tokenRepository.saveAll(validUserTokens)
    }

    @Throws(IOException::class)
    fun refreshToken(
        request: HttpServletRequest?,
        response: HttpServletResponse?
    ) {
        request ?: throw IllegalArgumentException("Request must not be null")
        response ?: throw IllegalArgumentException("Response must not be null")

        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        val userLogin: String
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return
        }
        val refreshToken = authHeader.substring(7)
        tokenRepository!!.findByToken(refreshToken)?: throw NotFoundException("Token not found")

        userLogin = jwtService!!.extractUsername(refreshToken)
        val user = repository!!.findByLogin(userLogin)
            .orElseThrow { NotFoundException("User not found") }
        if (jwtService.isTokenValid(refreshToken, user)) {
            // Invalidate and remove the old access token
            val oldAccessToken = tokenRepository!!.findTokenByUserAndToken(user, refreshToken)
            oldAccessToken?.let {
                it.revoked = true
                it.expired = true
                tokenRepository.save(it)
            }
            // Generate a new access token
            val newAccessToken = saveUserToken(user, jwtService.generateToken(user))
            val authResponse = AuthenticationResponse(
                newAccessToken.token,
                refreshToken
            )
            response.contentType = "application/json"
            ObjectMapper().writeValue(response.outputStream, authResponse)
        }
    }

    @Scheduled(fixedRate = 3600000) // runs every hour
    fun removeExpiredTokens() {
        val allTokens = tokenRepository!!.findAll()
        val expiredTokens = allTokens.filter {
            try {
                jwtService!!.isTokenExpired(it.token)
            } catch (e: AuthorisationRequired) {
                println("Failed to parse token: ${it.token}")
                false
            }
        }
        tokenRepository.deleteAll(expiredTokens)
    }
}
