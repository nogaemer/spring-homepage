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

data class ErrorInfo(
    val timestamp: String = Instant.now().toString(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val trace: String,
)