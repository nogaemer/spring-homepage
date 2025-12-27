package de.nogaemer.springhomepage.exceptions

import org.apache.catalina.connector.ClientAbortException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ClientAbortException::class)
    fun handleClientAbortException(ex: ClientAbortException): ResponseEntity<String> {
        // Log the exception
        println("Client aborted the connection: ${ex.message}")
        // Return a response indicating the client disconnected
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Client disconnected")
    }
}