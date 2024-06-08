package de.nogaemer.springhomepage.exceptions

import de.nogaemer.springhomepage.meals.models.Meal
import org.springframework.http.HttpStatus

data class AppException(
    override val message: String,
    val statusCode: HttpStatus
): RuntimeException(message)

data class IdNotFoundException(
    override val message: String,
): RuntimeException(message)

data class NotFoundException(
    override val message: String,
): RuntimeException(message)

data class TagNotFoundException(
    override val message: String,
): RuntimeException(message)

data class AlreadyReported(
    override val message: String,
    val `object`: Any
): RuntimeException(message)
