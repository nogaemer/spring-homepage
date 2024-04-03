package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.meals.models.Rating
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

    @GetMapping
    fun getRatings(): List<Rating> {
        service!!.create(
            Rating(
                mealId = ObjectId(),
                userId = ObjectId(),
                rating = 5
            )
        )
        return service!!.findAll()
    }

    @PostMapping
    fun createRating(
        @RequestBody rating: Rating
    ): ResponseEntity<Rating> {
        return ResponseEntity<Rating>(service!!.create(rating), HttpStatus.CREATED)
    }
}
