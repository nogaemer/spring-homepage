package de.nogaemer.springhomepage.exceptions

import de.nogaemer.springhomepage.logger.TextFormatter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.Instant

/**
 * Global exception handler for REST API endpoints.
 *
 * Intercepts all exceptions thrown by controllers and transforms them into
 * standardized JSON error responses with appropriate HTTP status codes.
 *
 * ## Exception Handling Strategy
 * - Maps domain exceptions to specific HTTP status codes
 * - Provides consistent error response format
 * - Logs errors with formatted console output
 * - Includes stack traces in response (consider removing in production)
 *
 * ## Supported Exception Types
 * - **IdNotFoundException**: 404 NOT_FOUND
 * - **UnitNotFoundException**: 404 NOT_FOUND
 * - **NotFoundException**: 404 NOT_FOUND
 * - **TagNotFoundException**: 404 NOT_FOUND
 * - **AlreadyReported**: 208 ALREADY_REPORTED (duplicate submission)
 * - **AuthorisationRequired**: 401 UNAUTHORIZED
 * - **AppException**: Custom status code from exception
 * - **All others**: 500 INTERNAL_SERVER_ERROR
 *
 * ## Error Response Format
 * All errors return [ErrorInfo] with:
 * - timestamp: ISO-8601 instant
 * - status: HTTP status code (numeric)
 * - error: HTTP status reason phrase
 * - message: Exception message
 * - path: Request URI
 * - trace: Stack trace (cleared to empty in response)
 *
 * ## Console Logging
 * Errors are logged to console with formatting:
 * - Exception class name (inverted/highlighted)
 * - Exception message (sub-error format)
 * - Full stack trace
 *
 * ## Production Considerations
 * - Stack traces in responses are security risk (expose internal structure)
 * - Consider removing or obfuscating traces for production
 * - Console logging may be insufficient (use proper logging framework)
 * - Add request correlation IDs for tracing
 *
 * @property formatter Text formatter for colorized console output
 *
 * @see ErrorInfo
 * @see de.nogaemer.springhomepage.logger.TextFormatter
 */
@ControllerAdvice
class ApiExceptionHandler {
    private val formatter: TextFormatter = TextFormatter()

    /**
     * Handles all throwable exceptions from controllers.
     *
     * Central exception handling method that:
     * 1. Maps exception type to HTTP status code
     * 2. Logs formatted error to console
     * 3. Creates standardized error response
     * 4. Returns ResponseEntity with appropriate status
     *
     * ## Exception to Status Mapping
     * Uses when expression to determine status:
     * - Domain exceptions (IdNotFoundException, etc.) → specific codes
     * - AppException → custom status from exception
     * - Unknown exceptions → 500 INTERNAL_SERVER_ERROR
     *
     * ## Side Effects
     * - Prints formatted error to System.out
     * - Clears exception stack trace (to avoid exposing in response)
     *
     * ## Error Info Contents
     * - **timestamp**: Current instant as ISO-8601 string
     * - **status**: Numeric HTTP status code
     * - **error**: HTTP status reason phrase (e.g., "Not Found")
     * - **message**: Exception message or default text
     * - **path**: Request URI where error occurred
     * - **trace**: Empty string (stack trace cleared)
     *
     * @param request HTTP request where exception occurred
     * @param exception The thrown exception
     * @return ResponseEntity with [ErrorInfo] and appropriate HTTP status
     */
    @ExceptionHandler(Throwable::class)
    fun handleErrors(request: HttpServletRequest, exception: Throwable): ResponseEntity<Any>{

        val code = when (exception) {
            is IdNotFoundException -> HttpStatus.NOT_FOUND
            is UnitNotFoundException -> HttpStatus.NOT_FOUND
            is NotFoundException -> HttpStatus.NOT_FOUND
            is TagNotFoundException -> HttpStatus.NOT_FOUND
            is AlreadyReported -> HttpStatus.ALREADY_REPORTED
            is AuthorisationRequired -> HttpStatus.UNAUTHORIZED
            is AppException -> exception.statusCode
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        val cause = exception.message ?: "An error occurred"
        val stacktrace = exception.stackTraceToString()

        println(formatter.errorInverted(exception.javaClass.simpleName))
        println(formatter.subErrorInverted("Exception") + cause)
        println(formatter.subErrorInverted("Stacktrace") + stacktrace + "\n")


        exception.stackTrace = arrayOf()
        val errorInfo = ErrorInfo(
            status = code.value(),
            error = code.reasonPhrase,
            trace = exception.stackTraceToString(),
            message = exception.message ?: "An error occurred",
            path = request.requestURI
        )

        return ResponseEntity.status(code).body(errorInfo)
    }
}

/**
 * Standardized error response data structure.
 *
 * Provides consistent error information format for all API error responses,
 * compatible with Spring Boot's default error response structure.
 *
 * ## Fields
 * - **timestamp**: When the error occurred (ISO-8601 format, auto-generated)
 * - **status**: HTTP status code (e.g., 404, 500)
 * - **error**: Human-readable status phrase (e.g., "Not Found")
 * - **message**: Detailed error description
 * - **path**: Request URI where error occurred
 * - **trace**: Stack trace (empty in this implementation)
 *
 * ## Default Values
 * - timestamp defaults to current instant
 * - Other fields required at construction
 *
 * ## JSON Structure
 * ```json
 * {
 *   "timestamp": "2024-01-15T10:30:45.123Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Meal not found",
 *   "path": "/api/v1/meals/123",
 *   "trace": ""
 * }
 * ```
 *
 * ## Usage
 * Created by [ApiExceptionHandler] for all error responses.
 * Can be extended with additional fields (e.g., errorCode, details).
 *
 * @property timestamp ISO-8601 timestamp of error occurrence
 * @property status HTTP status code
 * @property error HTTP status reason phrase
 * @property message Detailed error message
 * @property path Request URI
 * @property trace Stack trace (typically empty or redacted)
 */
data class ErrorInfo(
    val timestamp: String = Instant.now().toString(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val trace: String,
)