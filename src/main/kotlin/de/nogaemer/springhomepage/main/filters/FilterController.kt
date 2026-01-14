package de.nogaemer.springhomepage.main.filters

import de.nogaemer.springhomepage.main.meals.dto.MealCardDto
import de.nogaemer.springhomepage.user.UserResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/filters")
class FilterController(
    val filterService: FilterService
) {

    @GetMapping
    fun getFilters(): ResponseEntity<FilterResponse> {
        return ResponseEntity.ok().body(filterService.getFilters())
    }

    @GetMapping("/users")
    fun getUsers(
        @RequestParam name: String?
    ): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok().body(filterService.getUsers(name))
    }

    @GetMapping("/favorite")
    fun getMyFavoriteMeals(
        @RequestParam(defaultValue = "4") minRating: Int
    ): ResponseEntity<List<MealCardDto>> {
        return ResponseEntity.ok().body(filterService.getMyFavoriteMeals(minRating = minRating))
    }

    @GetMapping("/by-ingredients")
    fun getMealsByIngredients(
        @RequestParam ingredients: List<String>,
        @RequestParam(defaultValue = "0.5") minMatch: Double
    ): ResponseEntity<List<MealCardDto>> {
        return ResponseEntity.ok().body(filterService.getByIngredients(ingredients, minMatch))
    }


    @GetMapping("/by-name")
    fun getMealsByName(
        @RequestParam name: String
    ) : ResponseEntity<List<MealCardDto>>{
        return ResponseEntity.ok().body(filterService.searchByName(name))
    }




}