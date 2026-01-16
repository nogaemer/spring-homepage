package de.nogaemer.springhomepage.exceptions

import org.springframework.http.HttpStatus

/**
 * Generic application exception with customizable HTTP status code.
 *
 * Use this exception when you need to throw a domain-specific error with
 * a specific HTTP status that doesn't fit standard exception types.
 *
 * ## Use Cases
 * - Business logic violations requiring specific HTTP codes
 * - API-specific errors (e.g., 429 TOO_MANY_REQUESTS)
 * - Domain errors that don't fit NotFound/Unauthorized patterns
 *
 * ## Example
 * ```kotlin
 * throw AppException("Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS)
 * throw AppException("Payment required", HttpStatus.PAYMENT_REQUIRED)
 * ```
 *
 * ## Handling
 * Caught by [ApiExceptionHandler] and mapped to the specified status code.
 *
 * @property message Human-readable error description
 * @property statusCode HTTP status code to return in response
 *
 * @see ApiExceptionHandler
 */
data class AppException(
    override val message: String,
    val statusCode: HttpStatus
) : RuntimeException(message)

/**
 * Exception thrown when an entity with the specified ID is not found.
 *
 * Mapped to HTTP 404 NOT_FOUND by [ApiExceptionHandler].
 *
 * ## Common Scenarios
 * - GET /meals/{id} with non-existent ID
 * - DELETE /meals/{id} with non-existent ID
 * - Referenced entity lookup failures
 *
 * ## Example
 * ```kotlin
 * mealRepository.findById(id)
 *     .orElseThrow { IdNotFoundException("Meal not found") }
 * ```
 *
 * @property message Error description (typically includes entity type)
 */
data class IdNotFoundException(
    override val message: String,
) : RuntimeException(message)

/**
 * Exception thrown when a measurement unit is not found.
 *
 * Mapped to HTTP 404 NOT_FOUND by [ApiExceptionHandler].
 * Specialized version of IdNotFoundException for unit entities.
 *
 * ## Use Cases
 * - Unit lookup by ID failed
 * - Referenced unit doesn't exist in ingredient assignments
 *
 * ## Example
 * ```kotlin
 * unitRepository.findById(unitId)
 *     .orElseThrow { UnitNotFoundException("Unit not found: $unitId") }
 * ```
 *
 * @property message Error description
 */
data class UnitNotFoundException(
    override val message: String,
) : RuntimeException(message)

/**
 * Generic not-found exception for various entity types.
 *
 * Mapped to HTTP 404 NOT_FOUND by [ApiExceptionHandler].
 * Use when entity type is clear from context or message.
 *
 * ## Use Cases
 * - Search operations returning no results (when error appropriate)
 * - Generic resource lookups
 * - Fallback for missing entities
 *
 * @property message Error description
 */
data class NotFoundException(
    override val message: String,
) : RuntimeException(message)

/**
 * Exception thrown when user authorization is required but missing.
 *
 * Mapped to HTTP 401 UNAUTHORIZED by [ApiExceptionHandler].
 *
 * ## Common Scenarios
 * - Accessing protected resource without authentication
 * - Invalid or expired JWT token
 * - Missing authorization header
 *
 * ## Note
 * This differs from 403 FORBIDDEN which indicates authenticated but
 * insufficient permissions. This indicates authentication itself failed.
 *
 * @property message Error description
 */
data class AuthorisationRequired(
    override val message: String,
) : RuntimeException(message)

/**
 * Exception thrown when a tag entity is not found.
 *
 * Mapped to HTTP 404 NOT_FOUND by [ApiExceptionHandler].
 * Specialized version of IdNotFoundException for tag entities.
 *
 * ## Use Cases
 * - Tag lookup by ID or name failed
 * - Referenced tag doesn't exist in meal assignments
 *
 * @property message Error description
 */
data class TagNotFoundException(
    override val message: String,
) : RuntimeException(message)

/**
 * Exception thrown when user attempts duplicate submission.
 *
 * Mapped to HTTP 208 ALREADY_REPORTED by [ApiExceptionHandler].
 *
 * ## Use Cases
 * - User already rated this meal (one rating per user per meal)
 * - User already added note to this meal (one note per user per meal)
 * - Duplicate entity creation attempts
 *
 * ## Special Feature
 * Includes the existing object in the exception, allowing:
 * - Client to receive existing entity in error response
 * - Client to decide whether to update vs display error
 * - Avoids additional query to fetch existing entity
 *
 * ## Example
 * ```kotlin
 * existingRating?.let {
 *     throw AlreadyReported("User already rated this meal", it)
 * }
 * ```
 *
 * ## HTTP 208
 * ALREADY_REPORTED is typically used in WebDAV but repurposed here for
 * duplicate submission scenarios. Consider using 409 CONFLICT as alternative.
 *
 * @property message Error description
 * @property `object` The existing entity that caused the conflict
 *
 * @see de.nogaemer.springhomepage.main.meals.BaseService.create
 */
data class AlreadyReported(
    override val message: String,
    val `object`: Any
) : RuntimeException(message)
