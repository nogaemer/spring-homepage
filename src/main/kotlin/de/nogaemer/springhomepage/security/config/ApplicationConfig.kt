/*
 * Application security configuration that provides core authentication and authorization beans.
 *
 * This configuration class sets up the fundamental security components required for the application,
 * including user authentication, password encoding, and audit tracking. It serves as the foundation
 * for the security layer by defining how users are loaded, authenticated, and how passwords are secured.
 *
 * @author Spring Homepage Security Team
 * @since 1.0
 */
package de.nogaemer.springhomepage.security.config

import de.nogaemer.springhomepage.security.auditing.ApplicationAuditAware
import de.nogaemer.springhomepage.user.UserRepository
import lombok.RequiredArgsConstructor
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Core security configuration for authentication and authorization.
 *
 * Provides Spring Security beans for:
 * - User details loading from MongoDB
 * - Password encryption using BCrypt
 * - DAO-based authentication
 * - Audit tracking with ObjectId
 * - Authentication manager configuration
 */
@Configuration
@RequiredArgsConstructor
class ApplicationConfig {

    @Autowired
    private val repository: UserRepository? = null

    /**
     * Provides a UserDetailsService bean that loads user-specific data from MongoDB.
     *
     * This service is used by Spring Security to retrieve user information during authentication.
     * It queries the UserRepository by login username and returns the User entity which implements
     * UserDetails interface, providing authentication and authorization information.
     *
     * @return UserDetailsService implementation that loads users by login username
     * @throws UsernameNotFoundException if the user is not found in the database
     */
    @Bean
    fun userDetailsService(): UserDetailsService {
        return UserDetailsService { username: String? ->
            repository!!.findByLogin(username)
                .orElseThrow { UsernameNotFoundException("User not found") }
        }
    }

    /**
     * Configures and provides a DAO-based authentication provider.
     *
     * This provider uses the UserDetailsService to load user data and the PasswordEncoder
     * to verify credentials during authentication. It implements the standard username/password
     * authentication flow:
     * 1. Load user details by username
     * 2. Compare provided password (encoded) with stored password hash
     * 3. Grant authentication if credentials match
     *
     * @return AuthenticationProvider configured with user details service and password encoder
     */
    @Bean
    fun authenticationProvider(): AuthenticationProvider {
        val authProvider = DaoAuthenticationProvider()
        authProvider.setUserDetailsService(userDetailsService())
        authProvider.setPasswordEncoder(passwordEncoder())
        return authProvider
    }

    /**
     * Provides an AuditorAware bean for Spring Data JPA/MongoDB auditing.
     *
     * This bean supplies the current auditor (authenticated user's ID) for automatic
     * population of @CreatedBy and @LastModifiedBy audit fields in entities.
     * Uses ObjectId type to match MongoDB's native identifier format.
     *
     * @return AuditorAware implementation that retrieves the current authenticated user's ObjectId
     */
    @Bean
    fun auditorAware(): AuditorAware<ObjectId> {
        return ApplicationAuditAware()
    }

    /**
     * Exposes Spring Security's AuthenticationManager as a bean.
     *
     * The AuthenticationManager is the main interface for authentication operations in Spring Security.
     * This bean is required for manual authentication operations such as login endpoints where
     * authentication needs to be performed programmatically rather than through Spring Security filters.
     *
     * @param config Spring Security's authentication configuration
     * @return AuthenticationManager for processing authentication requests
     * @throws Exception if the authentication manager cannot be retrieved
     */
    @Bean
    @Throws(Exception::class)
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }

    /**
     * Provides a BCrypt password encoder for secure password hashing.
     *
     * BCrypt is a strong, adaptive hashing algorithm designed for password storage.
     * It automatically handles salt generation and incorporates a cost factor that
     * makes brute-force attacks computationally expensive. This encoder is used for:
     * - Hashing passwords during user registration
     * - Verifying passwords during authentication
     *
     * @return PasswordEncoder implementation using BCrypt algorithm
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
