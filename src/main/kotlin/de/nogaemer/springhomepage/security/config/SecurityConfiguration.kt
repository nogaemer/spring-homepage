/*
 * Main security configuration for the Spring Homepage application.
 *
 * This configuration establishes the complete security framework including:
 * - JWT-based authentication for stateless API security
 * - OAuth2 login integration for social authentication
 * - Role-based and permission-based authorization
 * - CORS configuration for cross-origin requests
 * - Public endpoint whitelisting (auth, swagger, websockets)
 * - Custom logout handling with token revocation
 *
 * Security Flow:
 * 1. Incoming requests are filtered through JwtAuthenticationFilter
 * 2. JWT tokens are validated and user authentication is established
 * 3. Role/permission checks are applied to protected endpoints
 * 4. OAuth2 authentication is available as an alternative login method
 * 5. Logout properly invalidates tokens and clears security context
 *
 * @author Spring Homepage Security Team
 * @since 1.0
 */
package de.nogaemer.springhomepage.security.config

import de.nogaemer.springhomepage.security.auth.OAuth2AuthenticationSuccessHandler
import de.nogaemer.springhomepage.user.Permission.*
import de.nogaemer.springhomepage.user.Role
import de.nogaemer.springhomepage.utils.EnvUtils
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Primary security configuration implementing JWT authentication and authorization.
 *
 * This class configures:
 * - Stateless session management (no HTTP sessions)
 * - JWT authentication filter chain
 * - Authorization rules for different user roles (ADMIN, MANAGER, USER)
 * - Permission-based access control (READ, CREATE, UPDATE, DELETE)
 * - OAuth2 social login integration
 * - CORS policy for allowed origins
 * - Public endpoint whitelist
 * - Token-based logout mechanism
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
class SecurityConfiguration {

    @Autowired
    private val jwtAuthFilter: JwtAuthenticationFilter? = null
    @Autowired
    private val authenticationProvider: AuthenticationProvider? = null
    @Autowired
    private val logoutHandler: LogoutHandler? = null

    /**
     * Configures the main security filter chain with JWT authentication and authorization rules.
     *
     * Security Chain Configuration:
     * 1. CSRF disabled (using JWT tokens which are immune to CSRF)
     * 2. CORS enabled with custom configuration
     * 3. Authorization rules:
     *    - White-listed URLs: public access (auth endpoints, swagger, websockets)
     *    - OPTIONS requests: public access (CORS preflight)
     *    - Management endpoints: ADMIN or MANAGER roles only
     *    - HTTP method-specific permissions for management endpoints
     *    - All other requests: require authentication
     * 4. OAuth2 login with custom success/failure handlers
     * 5. Stateless session management (no server-side sessions)
     * 6. JWT authentication filter before username/password filter
     * 7. Logout endpoint with token revocation
     *
     * Authorization Hierarchy:
     * - ADMIN: Full CRUD permissions (READ, CREATE, UPDATE, DELETE)
     * - MANAGER: Full CRUD permissions (READ, CREATE, UPDATE, DELETE)
     * - USER: Limited access (configured per endpoint)
     *
     * @param http HttpSecurity configuration builder
     * @param oauth2AuthenticationSuccessHandler Handler for successful OAuth2 authentication
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity, oauth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler): SecurityFilterChain {
        http
            // Disable CSRF as we're using JWT tokens which are not vulnerable to CSRF attacks
            .csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests { req ->
                // Public endpoints - no authentication required
                req.requestMatchers(*WHITE_LIST_URL)
                    .permitAll()
                    // Allow all CORS preflight OPTIONS requests
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // WebSocket endpoint for real-time search (JWT validated in handshake)
                    .requestMatchers("/ws-search/**").permitAll()
                    // Management endpoints require ADMIN or MANAGER role
                    .requestMatchers("/api/v1/management/**").hasAnyRole(Role.ADMIN.name, Role.MANAGER.name)
                    // Fine-grained permission checks for management endpoints by HTTP method
                    .requestMatchers(HttpMethod.GET, "/api/v1/management/**")
                    .hasAnyAuthority(ADMIN_READ.name, MANAGER_READ.name)
                    .requestMatchers(HttpMethod.POST, "/api/v1/management/**")
                    .hasAnyAuthority(ADMIN_CREATE.name, MANAGER_CREATE.name)
                    .requestMatchers(HttpMethod.PUT, "/api/v1/management/**")
                    .hasAnyAuthority(ADMIN_UPDATE.name, MANAGER_UPDATE.name)
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/management/**")
                    .hasAnyAuthority(ADMIN_DELETE.name, MANAGER_DELETE.name)
                    // All other requests require authentication
                    .anyRequest()
                    .authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .successHandler(oauth2AuthenticationSuccessHandler)
                    .failureHandler(authenticationFailureHandler())
            }
            .sessionManagement { session ->
                // Stateless sessions - no HTTP session created or used
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            .authenticationProvider(authenticationProvider)
            // Add JWT filter before Spring Security's username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .logout { logout ->
                logout.logoutUrl("/api/v1/auth/logout")
                    // Custom logout handler revokes the JWT token
                    .addLogoutHandler(logoutHandler)
                    // Clear security context after successful logout
                    .logoutSuccessHandler { _: HttpServletRequest?, _: HttpServletResponse?, _: Authentication? -> SecurityContextHolder.clearContext() }
            }

        return http.build()
    }

    /**
     * Provides a custom authentication failure handler for OAuth2 login failures.
     *
     * When OAuth2 authentication fails (e.g., user denies access, token exchange fails),
     * this handler redirects the user to an error endpoint in the client application.
     * It attempts to use the Origin header from the request to determine the redirect URL,
     * falling back to the configured CLIENT_BASE_URL if the header is not present.
     *
     * @return AuthenticationFailureHandler that redirects to error endpoint
     * @throws IllegalStateException if CLIENT_BASE_URL environment variable is not set
     */
    @Bean
    fun authenticationFailureHandler(): AuthenticationFailureHandler {
        val baseUrl = EnvUtils.getEnvVariable("CLIENT_BASE_URL") ?: throw IllegalStateException("CLIENT_BASE_URL environment variable is not set")

        return AuthenticationFailureHandler { request, response, _ ->
            // Redirect to client application error page on OAuth2 authentication failure
            response.sendRedirect("${request.getHeader("Origin") ?: baseUrl}/api/v1/auth/error")
        }
    }

    /**
     * Configures Cross-Origin Resource Sharing (CORS) policy.
     *
     * CORS configuration allows the Spring API to accept requests from specified client origins.
     * This is essential for browser-based applications that are hosted on different domains
     * than the API server.
     *
     * Configuration details:
     * - Allowed origins: localhost:5173 (dev) and production domain
     * - Allowed methods: GET, POST, PUT, DELETE, OPTIONS
     * - Allowed headers: All headers (*)
     * - Credentials: Enabled (allows cookies and authorization headers)
     *
     * @return CorsConfigurationSource with configured CORS policy
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = listOf(
            "http://localhost:5173",
            "https://localhost:5173",
            "https://meal-planer-react.appwrite.network"
        )
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

    companion object {
        /**
         * List of URL patterns that do not require authentication.
         *
         * Includes:
         * - Root endpoint
         * - Authentication endpoints (login, register, refresh token, OAuth2)
         * - API documentation endpoints (Swagger/OpenAPI)
         * - Swagger UI resources
         *
         * These endpoints are publicly accessible without a valid JWT token.
         */
        private val WHITE_LIST_URL = arrayOf(
            "/",
            "/api/v1/auth/**",
            "/v2/api-docs",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui/**",
            "/webjars/**",
            "/swagger-ui.html"
        )
    }
}

