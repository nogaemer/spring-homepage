package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.meals.dto.MealDto
import de.nogaemer.springhomepage.meals.import.MealImportUrl
import de.nogaemer.springhomepage.meals.models.Meal
import de.nogaemer.springhomepage.meals.models.MealImportMethod
import org.bson.types.ObjectId
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
    fun getMealsByName(
        @RequestParam(
            value = "name",
            defaultValue = ""
        ) name: String
    ): ResponseEntity<List<Meal>> {
        return ResponseEntity<List<Meal>>(service?.searchByName(name), HttpStatus.OK)
    }

    @GetMapping("/{id}")
    fun getSingleMeal(
        @PathVariable id: ObjectId?
    ): ResponseEntity<Meal> {
        id ?: throw IllegalArgumentException("Id is null")

        val response = service!!.findById(id)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{id}")
    fun updateMeal(
        @PathVariable id: ObjectId?,
        @RequestBody meal: MealDto
    ): ResponseEntity<Meal> {
        id ?: throw IllegalArgumentException("Id is null")

        return ResponseEntity<Meal>(service!!.update(id, meal), HttpStatus.OK)
    }

    @PostMapping
    fun createMeal(
        @RequestBody meal: MealDto
    ): ResponseEntity<Meal> {
        return ResponseEntity<Meal>(service!!.create(meal), HttpStatus.CREATED)
    }

    @DeleteMapping("/{id}")
    fun deleteMeal(
        @PathVariable id: ObjectId?
    ): ResponseEntity<Void> {
        id ?: throw IllegalArgumentException("Id is null")

        service!!.deleteById(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}

@RestController
@RequestMapping("/api/v1/import")
class MealImportController {
    @Autowired
    private val service: MealService? = null

    @GetMapping("/meal")
    fun getMeal(
        @RequestParam(value = "importTag", defaultValue = "chefkoch") importTag: String,
        @RequestParam(value = "url") url: String?,
    ): ResponseEntity<Meal> {
        url ?: throw IllegalArgumentException("Url is null")

        val tag = MealImportMethod.valueOf(importTag.uppercase())

        return ResponseEntity(service!!.importMealAsync(tag, url, false).get(), HttpStatus.CREATED)
    }

    @PostMapping("/meal")
    fun createMeal(
        @RequestParam(value = "importTag", defaultValue = "chefkoch") importTag: String,
        @RequestBody import: MealImportUrl
    ): ResponseEntity<Meal> {
        val tag = MealImportMethod.valueOf(importTag.uppercase())

        return ResponseEntity(service!!.importMealAsync(tag, import.url).get(), HttpStatus.CREATED)
    }
}