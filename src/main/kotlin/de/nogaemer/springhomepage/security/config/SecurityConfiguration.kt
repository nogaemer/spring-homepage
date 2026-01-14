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


    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity, oauth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests { req ->
                req.requestMatchers(*WHITE_LIST_URL)
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/ws-search/**").permitAll()
                    .requestMatchers("/api/v1/management/**").hasAnyRole(Role.ADMIN.name, Role.MANAGER.name)
                    .requestMatchers(HttpMethod.GET, "/api/v1/management/**")
                    .hasAnyAuthority(ADMIN_READ.name, MANAGER_READ.name)
                    .requestMatchers(HttpMethod.POST, "/api/v1/management/**")
                    .hasAnyAuthority(ADMIN_CREATE.name, MANAGER_CREATE.name)
                    .requestMatchers(HttpMethod.PUT, "/api/v1/management/**")
                    .hasAnyAuthority(ADMIN_UPDATE.name, MANAGER_UPDATE.name)
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/management/**")
                    .hasAnyAuthority(ADMIN_DELETE.name, MANAGER_DELETE.name)
                    .anyRequest()
                    .authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .successHandler(oauth2AuthenticationSuccessHandler)
                    .failureHandler(authenticationFailureHandler())
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .logout { logout ->
                logout.logoutUrl("/api/v1/auth/logout")
                    .addLogoutHandler(logoutHandler)
                    .logoutSuccessHandler { _: HttpServletRequest?, _: HttpServletResponse?, _: Authentication? -> SecurityContextHolder.clearContext() }
            }

        return http.build()
    }

    @Bean
    fun authenticationFailureHandler(): AuthenticationFailureHandler {
        val baseUrl = EnvUtils.getEnvVariable("CLIENT_BASE_URL") ?: throw IllegalStateException("CLIENT_BASE_URL environment variable is not set")

        return AuthenticationFailureHandler { request, response, _ ->
            // Redirect to your React app with an error
            response.sendRedirect("${request.getHeader("Origin") ?: baseUrl}/api/v1/auth/error")
        }
    }

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

