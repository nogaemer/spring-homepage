package de.nogaemer.springhomepage.main.search

import de.nogaemer.springhomepage.main.meals.UnifiedMealSearchService
import de.nogaemer.springhomepage.main.meals.dto.UnifiedMealSearchRequest
import de.nogaemer.springhomepage.main.meals.dto.UnifiedMealSearchResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/search")
class SearchController(
    private val unifiedMealSearchService: UnifiedMealSearchService,
    private val ingredientSuggestionService: IngredientSuggestionService
) {
    @PostMapping("/meals")
    fun searchMeals(@RequestBody request: UnifiedMealSearchRequest): ResponseEntity<UnifiedMealSearchResponse> {
        return ResponseEntity.ok(unifiedMealSearchService.search(request))
    }

    @GetMapping("/did-you-mean")
    fun didYouMean(
        @RequestParam query: String,
        @RequestParam(defaultValue = "5") limit: Int,
        @RequestParam(defaultValue = "2") maxDistance: Int
    ): ResponseEntity<DidYouMeanResponse> {
        return ResponseEntity.ok(ingredientSuggestionService.suggest(query, limit, maxDistance))
    }
}
