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

@Service
@RequiredArgsConstructor
class AuthenticationService(
    private val repository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager,
) {

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

    private fun extractEmail(oauth2User: OAuth2User): String? {
        return oauth2User.getAttribute<String>("email")
            ?: oauth2User.getAttribute<String>("login") // GitHub
    }

    private fun extractName(oauth2User: OAuth2User): String? {
        return oauth2User.getAttribute<String>("name")
            ?: oauth2User.getAttribute<String>("login") // GitHub fallback
            ?: oauth2User.getAttribute<String>("given_name") // Google fallback
    }

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

    private fun revokeAllUserTokens(user: User) {
        val validUserTokens = tokenRepository.findAllValidTokenByUser(user.id!!)
        if (validUserTokens.isEmpty()) return
        validUserTokens.forEach(Consumer { token: Token ->
            token.expired = true
            token.revoked = true
        })
        tokenRepository.saveAll(validUserTokens)
    }

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

@Component
class OAuth2AuthenticationSuccessHandler(
    private val authenticationService: AuthenticationService,
) : AuthenticationSuccessHandler {
    val baseUrl = EnvUtils.getEnvVariable("CLIENT_BASE_URL")
        ?: throw IllegalStateException("CLIENT_BASE_URL environment variable is not set")

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