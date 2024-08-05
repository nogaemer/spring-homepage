package de.nogaemer.springhomepage.security.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.IOException

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class AuthenticationController {

    @Autowired
    private val service: AuthenticationService? = null

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
}
