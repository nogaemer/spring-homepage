package de.nogaemer.springhomepage.security.auth

import de.nogaemer.springhomepage.exceptions.NotFoundException
import de.nogaemer.springhomepage.security.config.JwtService
import de.nogaemer.springhomepage.user.User
import de.nogaemer.springhomepage.user.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class AuthenticationController(
    private val repository: UserRepository,
) {

    @Autowired
    private val service: AuthenticationService? = null

    @Autowired
    private val jwtTokenService: JwtService? = null

    @PostMapping("/register")
    @PreAuthorize("hasAuthority('admin:read')")
    fun register(
        @RequestBody request: RegisterRequest?
    ): ResponseEntity<AuthenticationResponse> {
        return ResponseEntity.ok(service!!.register(request))
    }

    @PostMapping("/authenticate")
    fun authenticate(
        @RequestBody request: AuthenticationRequest?
    ): ResponseEntity<AuthenticationResponse> {
        return ResponseEntity.ok(service!!.authenticate(request))
    }

    @GetMapping("/refresh-token")
    @Throws(IOException::class)
    fun refreshToken(
        request: HttpServletRequest?
    ): ResponseEntity<AuthenticationResponse> {
        return ResponseEntity.ok(service!!.refreshToken(request))
    }

    @GetMapping("/login/{provider}")
    fun login(@PathVariable provider: String, response: HttpServletResponse) {
        // This will redirect to OAuth2 provider
        response.sendRedirect("/oauth2/authorization/$provider")
    }

}
