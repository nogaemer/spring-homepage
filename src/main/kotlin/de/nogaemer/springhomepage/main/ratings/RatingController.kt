/**
 * REST API controller for rating management operations.
 *
 * Provides endpoints for creating, reading, updating, and deleting meal ratings.
 * All endpoints are prefixed with /api/v1/ratings and require JWT authentication.
 */
package de.nogaemer.springhomepage.main.ratings

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.main.notes.Note
import de.nogaemer.springhomepage.security.config.JwtService
import jakarta.servlet.http.HttpServletRequest
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller handling HTTP requests for rating operations.
 *
 * @property service The rating service providing business logic
 * @property jwtService Service for extracting user from JWT tokens
 */
@RestController
@RequestMapping("/api/v1/ratings")
class RatingController(
    val service: RatingService,
    val jwtService: JwtService
) {

    /**
     * Retrieves all ratings in the system (admin function).
     *
     * @return List of all ratings
     */
    @GetMapping
    fun getRatings(): List<Rating> {
        return service.findAll()
    }

    /**
     * Retrieves all ratings for a specific meal with user details.
     *
     * @param id The meal ID
     * @return ResponseEntity containing rating response with user info and average
     */
    @GetMapping("/{id}")
    fun getRating(
        @PathVariable id: String
    ): ResponseEntity<RatingResponse> {
        return ResponseEntity.ok(service.getRatingsByMealId(ObjectId(id)))
    }

    /**
     * Creates a new rating for a meal.
     *
     * Extracts the user ID from the JWT token and associates it with the rating.
     * Automatically recalculates the meal's average rating.
     *
     * @param rating The rating data (userId will be set from token)
     * @param request The HTTP request containing JWT token
     * @return ResponseEntity containing the created rating
     */
    @PostMapping
    fun createRating(
        @RequestBody rating: Rating,
        request: HttpServletRequest
    ): ResponseEntity<Rating> {
        val user = jwtService.extractUserFromRequest(request)
            ?: throw RuntimeException("User not found")

        rating.userId = user.id!!
        return ResponseEntity<Rating>(service.create(rating), HttpStatus.CREATED)
    }

    /**
     * Updates an existing rating.
     *
     * @param id The rating ID
     * @param rating The updated rating data
     * @return ResponseEntity containing the updated rating
     */
    @PutMapping("/{id}")
    fun updateRating(
        @PathVariable id: String,
        @RequestBody rating: Rating
    ): ResponseEntity<Rating> {
        return ResponseEntity.ok(service.update(ObjectId(id), rating))
    }

    /**
     * Deletes a rating.
     *
     * Automatically recalculates the meal's average rating after deletion.
     *
     * @param id The rating ID
     * @return Empty 200 OK response
     */
    @DeleteMapping("/{id}")
    fun deleteRating(
        @PathVariable id: String
    ): ResponseEntity<*> {
        service.delete(ObjectId(id))
        return ResponseEntity.ok().build<String>()
    }
}
