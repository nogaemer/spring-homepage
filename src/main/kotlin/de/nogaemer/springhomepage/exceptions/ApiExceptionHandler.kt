package de.nogaemer.springhomepage.exceptions

import de.nogaemer.springhomepage.logger.TextFormatter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.Instant

@ControllerAdvice
class ApiExceptionHandler {
    private val formatter: TextFormatter = TextFormatter()

    @ExceptionHandler(Throwable::class)
    fun handleErrors(request: HttpServletRequest, exception: Throwable): ResponseEntity<Any>{

        val code = when (exception) {
            is IdNotFoundException -> HttpStatus.NOT_FOUND
            is TagNotFoundException -> HttpStatus.NOT_FOUND
            is AlreadyReported -> HttpStatus.ALREADY_REPORTED
            is AppException -> exception.statusCode
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }


        exception.stackTrace = arrayOf()
        val errorInfo = ErrorInfo(
            status = code.value(),
            error = code.reasonPhrase,
            trace = exception.stackTraceToString(),
            message = exception.message ?: "An error occurred",
            path = request.requestURI
        )

        println(exception.localizedMessage)
        println(exception.stackTraceToString())
        println(exception.cause)

        return ResponseEntity.status(code).body(errorInfo)
    }
}

data class ErrorInfo(
    val timestamp: String = Instant.now().toString(),
    val status: Int,
    val error: String,
    val trace: String,
    val message: String,
    val path: String
)