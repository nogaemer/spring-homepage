package de.nogaemer.springhomepage.main.meals

import de.nogaemer.springhomepage.main.meals.dto.MealCardDto
import de.nogaemer.springhomepage.main.meals.dto.MealDto
import de.nogaemer.springhomepage.main.meals.dto.UnifiedMealSearchRequest
import de.nogaemer.springhomepage.main.meals.dto.UnifiedMealSearchResponse
import de.nogaemer.springhomepage.main.meals.import.MealImportUrl
import de.nogaemer.springhomepage.main.meals.models.Meal
import de.nogaemer.springhomepage.main.meals.models.MealImportMethod
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/meals")
class MealController(
    private val unifiedMealSearchService: UnifiedMealSearchService, private val service: MealService
) {


    @PostMapping("/search")
    fun searchMeals(
        @RequestBody request: UnifiedMealSearchRequest
    ): ResponseEntity<UnifiedMealSearchResponse> {
        return ResponseEntity.ok(unifiedMealSearchService.search(request))
    }

    @GetMapping("/all")
    fun getAllMeals(): ResponseEntity<List<MealCardDto>> {
        return ResponseEntity<List<MealCardDto>>(service.findAll(), HttpStatus.OK)
    }

    @GetMapping("/{id}")
    fun getSingleMeal(
        @PathVariable id: ObjectId?
    ): ResponseEntity<Meal> {
        id ?: throw IllegalArgumentException("Id is null")

        val response = service.findById(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/byFilter")
    fun getFiltertMeals(
        @RequestParam name: String?, @RequestParam users: String?, @RequestParam tags: String?, @RequestParam time: Int?
    ): ResponseEntity<List<MealCardDto>> {
        val response = service.filterMeals(name, users, tags, time)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{id}")
    fun updateMeal(
        @PathVariable id: ObjectId?, @RequestBody meal: MealDto
    ): ResponseEntity<Meal> {
        id ?: throw IllegalArgumentException("Id is null")

        return ResponseEntity<Meal>(service.update(id, meal), HttpStatus.OK)
    }

    @PostMapping
    fun createMeal(
        @RequestBody meal: MealDto
    ): ResponseEntity<Meal> {
        return ResponseEntity<Meal>(service.create(meal), HttpStatus.CREATED)
    }

    @DeleteMapping("/{id}")
    fun deleteMeal(
        @PathVariable id: ObjectId?
    ): ResponseEntity<Void> {
        id ?: throw IllegalArgumentException("Id is null")

        service.deleteById(id)
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