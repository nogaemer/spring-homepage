/**
 * REST API controller for search operations.
 *
 * Provides endpoints for meal search and spelling suggestions.
 * All endpoints are prefixed with /api/v1/search.
 */
package de.nogaemer.springhomepage.main.search

import de.nogaemer.springhomepage.main.meals.UnifiedMealSearchService
import de.nogaemer.springhomepage.main.meals.dto.UnifiedMealSearchRequest
import de.nogaemer.springhomepage.main.meals.dto.UnifiedMealSearchResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller handling search-related HTTP requests.
 *
 * @property unifiedMealSearchService Service for executing meal searches
 * @property ingredientSuggestionService Service for generating spelling suggestions
 */
@RestController
@RequestMapping("/api/v1/search")
class SearchController(
    private val unifiedMealSearchService: UnifiedMealSearchService,
    private val ingredientSuggestionService: IngredientSuggestionService
) {
    /**
     * Searches for meals using the unified search algorithm.
     *
     * Supports filtering by name, tags, time, ingredients, user ratings, and more.
     * See UnifiedMealSearchRequest for all available filter options.
     *
     * @param request The search request with filters and sorting options
     * @return ResponseEntity containing search results with meal cards
     */
    @PostMapping("/meals")
    fun searchMeals(@RequestBody request: UnifiedMealSearchRequest): ResponseEntity<UnifiedMealSearchResponse> {
        return ResponseEntity.ok(unifiedMealSearchService.search(request))
    }

    /**
     * Generates "Did you mean?" suggestions for misspelled ingredient names.
     *
     * Uses Levenshtein distance to find similar ingredient names within the specified
     * edit distance threshold.
     *
     * @param query The ingredient name to find suggestions for
     * @param limit Maximum number of suggestions to return (default: 5)
     * @param maxDistance Maximum Levenshtein distance threshold (default: 2)
     * @return ResponseEntity containing the query and suggested corrections
     */
    @GetMapping("/did-you-mean")
    fun didYouMean(
        @RequestParam query: String,
        @RequestParam(defaultValue = "5") limit: Int,
        @RequestParam(defaultValue = "2") maxDistance: Int
    ): ResponseEntity<DidYouMeanResponse> {
        return ResponseEntity.ok(ingredientSuggestionService.suggest(query, limit, maxDistance))
    }
}
