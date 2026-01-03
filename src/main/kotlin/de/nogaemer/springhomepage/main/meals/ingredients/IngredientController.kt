package de.nogaemer.springhomepage.main.meals.ingredients

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/ingredients")
class IngredientController(val service: IngredientService) {

    @GetMapping
    fun list(
        @RequestParam(value = "limit", defaultValue = "20") limit: Int,
        @RequestParam(value = "offset", defaultValue = "0") offset: Int,
        @RequestParam(value = "query", defaultValue = "") query: String
    ): ResponseEntity<List<Ingredient>> {
        return ResponseEntity<List<Ingredient>>(service.getIngredients(limit, offset, query), HttpStatus.OK)
    }

    @PostMapping
    fun save(@RequestBody ingredient: Ingredient): ResponseEntity<Ingredient> {
        return ResponseEntity<Ingredient>(service.saveIngredient(ingredient), HttpStatus.OK)
    }

    @PostMapping("/bulk")
    fun saveAll(@RequestBody ingredients: List<Ingredient>): ResponseEntity<List<Ingredient>> {
        return ResponseEntity<List<Ingredient>>(service.saveIngredients(ingredients), HttpStatus.OK)
    }

    @DeleteMapping
    fun remove(@RequestBody ingredient: Ingredient) {
        service.removeIngredient(ingredient)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): ResponseEntity<Ingredient> {
        val ing = service.findById(id)
        return if (ing != null) ResponseEntity(ing, HttpStatus.OK) else ResponseEntity(HttpStatus.NOT_FOUND)
    }
}
