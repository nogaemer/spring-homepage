/**
 * REST API controller for ingredient management operations.
 *
 * Provides endpoints for searching, creating, updating, and deleting ingredients.
 * All endpoints are prefixed with /api/v1/ingredients.
 */
package de.nogaemer.springhomepage.main.ingredients

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller handling HTTP requests for ingredient operations.
 *
 * @property service The ingredient service providing business logic
 */
@RestController
@RequestMapping("/api/v1/ingredients")
class IngredientController(val service: IngredientService) {

    /**
     * Lists ingredients with optional search and pagination.
     *
     * @param limit Maximum number of results (default: 20)
     * @param offset Page offset for pagination (default: 0)
     * @param query Search query for filtering by name or category (default: empty, returns all)
     * @return ResponseEntity containing list of matching ingredients
     */
    @GetMapping
    fun list(
        @RequestParam(value = "limit", defaultValue = "20") limit: Int,
        @RequestParam(value = "offset", defaultValue = "0") offset: Int,
        @RequestParam(value = "query", defaultValue = "") query: String
    ): ResponseEntity<List<Ingredient>> {
        return ResponseEntity<List<Ingredient>>(service.getIngredients(limit, offset, query), HttpStatus.OK)
    }

    /**
     * Creates or updates a single ingredient.
     *
     * @param ingredient The ingredient data to save
     * @return ResponseEntity containing the saved ingredient with generated ID
     */
    @PostMapping
    fun save(@RequestBody ingredient: Ingredient): ResponseEntity<Ingredient> {
        return ResponseEntity<Ingredient>(service.saveIngredient(ingredient), HttpStatus.OK)
    }

    /**
     * Creates or updates multiple ingredients in bulk.
     *
     * @param ingredients List of ingredients to save
     * @return ResponseEntity containing list of saved ingredients with generated IDs
     */
    @PostMapping("/bulk")
    fun saveAll(@RequestBody ingredients: List<Ingredient>): ResponseEntity<List<Ingredient>> {
        return ResponseEntity<List<Ingredient>>(service.saveIngredients(ingredients), HttpStatus.OK)
    }

    /**
     * Deletes an ingredient from the database.
     *
     * @param ingredient The ingredient to remove
     */
    @DeleteMapping
    fun remove(@RequestBody ingredient: Ingredient) {
        service.removeIngredient(ingredient)
    }

    /**
     * Retrieves a single ingredient by its ID.
     *
     * @param id The ingredient ID as a string
     * @return ResponseEntity containing the ingredient if found, 404 NOT_FOUND otherwise
     */
    @GetMapping("/{id}")
    fun get(@PathVariable id: String): ResponseEntity<Ingredient> {
        val ing = service.findById(id)
        return if (ing != null) ResponseEntity(ing, HttpStatus.OK) else ResponseEntity(HttpStatus.NOT_FOUND)
    }
}
