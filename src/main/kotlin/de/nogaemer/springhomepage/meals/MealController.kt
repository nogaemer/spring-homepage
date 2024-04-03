package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.meals.models.Meal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/meals")
class MealController {
    @Autowired
    private val service: MealService? = null

    @GetMapping
    fun getMovies(): ResponseEntity<List<Meal>> {
        return ResponseEntity<List<Meal>>(service?.findAll(), HttpStatus.OK)
    }

    @GetMapping("/{name}")
    fun getSingleMeal(
        @PathVariable name: String?
    ): ResponseEntity<Meal> {
        val response = service!!.findByName(name)
        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun createMeal(
        @RequestBody meal: Meal
    ): ResponseEntity<Meal> {
        println(meal)
        return ResponseEntity<Meal>(service!!.create(meal), HttpStatus.CREATED)
    }
}