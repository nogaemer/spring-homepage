package de.nogaemer.springhomepage.meals.ratings

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.meals.notes.Note
import de.nogaemer.springhomepage.security.config.JwtService
import jakarta.servlet.http.HttpServletRequest
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/v1/ratings")
class RatingController(
    val service: RatingService,
    val jwtService: JwtService
) {

    @GetMapping
    fun getRatings(): List<Rating> {
        return service.findAll()
    }

    @GetMapping("/{id}")
    fun getRating(
        @PathVariable id: String
    ): ResponseEntity<List<RatingResponse>> {
        return ResponseEntity.ok(service.getRatingsByMealId(ObjectId(id)))
    }

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

    @PutMapping("/{id}")
    fun updateRating(
        @PathVariable id: String,
        @RequestBody rating: Rating
    ): ResponseEntity<Rating> {
        return ResponseEntity.ok(service.update(ObjectId(id), rating))
    }

    @DeleteMapping("/{id}")
    fun deleteRating(
        @PathVariable id: String
    ): ResponseEntity<*> {
        service.delete(ObjectId(id))
        return ResponseEntity.ok().build<String>()
    }
}
