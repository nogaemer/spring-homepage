package de.nogaemer.springhomepage.meals.ratings

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.security.config.JwtService
import jakarta.servlet.http.HttpServletRequest
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/v1/ratings")
class RatingController {
    @Autowired
    private val service: RatingService? = null

    @Autowired
    private val jwtService: JwtService? = null

    @GetMapping
    fun getRatings(): List<Rating> {
        service!!.create(
            Rating(
                mealId = ObjectId(),
                rating = 5
            )
        )
        return service.findAll()
    }

    @PostMapping
    fun createRating(
        @RequestBody rating: Rating,
        request: HttpServletRequest
    ): ResponseEntity<Rating> {
        val user = jwtService!!.extractUserFromRequest(request)
            ?: throw RuntimeException("User not found")

        rating.userId = user.id!!
        return ResponseEntity<Rating>(service!!.create(rating), HttpStatus.CREATED)
    }

    @DeleteMapping("/{id}")
    fun deleteRating(
        @PathVariable id: String
    ): ResponseEntity<*> {
        println(id)
        service!!.delete(ObjectId(id))
        return ResponseEntity.ok().build<String>()
    }
}
