package de.nogaemer.springhomepage.exceptions

import org.apache.catalina.connector.ClientAbortException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus

/**
 * Global exception handler for specific runtime exceptions.
 *
 * Handles exceptions that occur outside normal API request processing,
 * particularly client disconnection scenarios during response streaming.
 *
 * ## Purpose
 * Prevents cluttering logs with stack traces for expected client behavior
 * (e.g., user closing browser tab during file download or long-running request).
 *
 * ## Handled Exceptions
 * - **ClientAbortException**: Thrown when client closes connection prematurely
 *
 * ## Complementary to ApiExceptionHandler
 * Works alongside [ApiExceptionHandler]:
 * - ApiExceptionHandler: Domain/business logic exceptions
 * - GlobalExceptionHandler: Infrastructure/runtime exceptions
 *
 * ## @RestControllerAdvice
 * Applies to all @RestController classes in the application.
 * Similar to @ControllerAdvice but assumes @ResponseBody on all methods.
 *
 * @see ApiExceptionHandler
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * Handles client connection abort exceptions.
     *
     * Called when client closes connection before response is fully sent.
     * This is normal behavior (not an error) when users navigate away or
     * cancel requests.
     *
     * ## Common Scenarios
     * - User closes browser tab during file download
     * - User navigates away during long API request
     * - Network interruption on client side
     * - Client timeout
     *
     * ## Handling Strategy
     * - Logs the event (println) rather than full stack trace
     * - Returns 503 SERVICE_UNAVAILABLE status
     * - Response may not reach client (connection already closed)
     *
     * ## Response Status
     * HTTP 503 SERVICE_UNAVAILABLE indicates the server cannot complete
     * the request due to client disconnection. Alternative could be
     * 499 Client Closed Request (non-standard nginx status).
     *
     * ## Logging
     * Currently uses println. Consider upgrading to proper logging framework
     * (SLF4J) for production with appropriate log level (INFO or DEBUG, not ERROR).
     *
     * @param ex The ClientAbortException thrown by Tomcat
     * @return ResponseEntity with SERVICE_UNAVAILABLE status (may not reach client)
     */
    @ExceptionHandler(ClientAbortException::class)
    fun handleClientAbortException(ex: ClientAbortException): ResponseEntity<String> {
        // Log the exception
        println("Client aborted the connection: ${ex.message}")
        // Return a response indicating the client disconnected
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Client disconnected")
    }
}