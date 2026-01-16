/**
 * Controller for Render.com hosting platform health checks and monitoring.
 *
 * This minimal controller provides a simple endpoint to verify that the application
 * is successfully deployed and responding to requests. Render.com and other hosting
 * platforms often use such endpoints for:
 * - Initial deployment verification
 * - Health checks and uptime monitoring
 * - Load balancer connectivity tests
 *
 * The controller responds to the root path with a basic message to confirm the
 * application is running.
 */
package de.nogaemer.springhomepage.hosting

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller providing a health check endpoint for hosting platforms.
 *
 * This controller is intentionally minimal and unmapped to any specific path,
 * allowing it to respond to the root URL of the application.
 */
@RestController
class RenderOnlineController {

    /**
     * Health check endpoint that returns a simple greeting message.
     *
     * This endpoint is used by hosting platforms like Render.com to verify that:
     * - The application has successfully started
     * - The web server is accepting HTTP requests
     * - The application is responsive and healthy
     *
     * @return ResponseEntity containing "Hello World" message with HTTP 200 OK status
     */
    @GetMapping()
    fun render(): ResponseEntity<String> {
        return ResponseEntity.ok().body("Hello World")
    }
}