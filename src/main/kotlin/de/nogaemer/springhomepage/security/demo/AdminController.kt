package de.nogaemer.springhomepage.security.demo

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController {
    @GetMapping
    @PreAuthorize("hasAuthority('admin:read')")
    fun get(): String {
        return "GET:: admin controller"
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:create')")
    @Hidden
    fun post(): String {
        return "POST:: admin controller"
    }

    @PutMapping
    @PreAuthorize("hasAuthority('admin:update')")
    @Hidden
    fun put(): String {
        return "PUT:: admin controller"
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('admin:delete')")
    @Hidden
    fun delete(): String {
        return "DELETE:: admin controller"
    }
}
