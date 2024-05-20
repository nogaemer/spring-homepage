package de.nogaemer.springhomepage.security.demo

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/management")
@Tag(name = "Management")
class ManagementController {
    @Operation(
        description = "Get endpoint for manager",
        summary = "This is a summary for management get endpoint",
        responses = [ApiResponse(
            description = "Success",
            responseCode = "200"
        ), ApiResponse(description = "Unauthorized / Invalid Token", responseCode = "403")]
    )
    @GetMapping
    fun get(): String {
        return "GET:: management controller"
    }

    @PostMapping
    fun post(): String {
        return "POST:: management controller"
    }

    @PutMapping
    fun put(): String {
        return "PUT:: management controller"
    }

    @DeleteMapping
    fun delete(): String {
        return "DELETE:: management controller"
    }
}
