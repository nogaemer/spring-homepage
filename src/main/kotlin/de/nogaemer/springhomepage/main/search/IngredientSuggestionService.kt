package de.nogaemer.springhomepage.main.search

import de.nogaemer.springhomepage.main.ingredients.IngredientRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import kotlin.math.max

data class DidYouMeanResponse(
    val query: String,
    val suggestions: List<String>
)

@Service
class IngredientSuggestionService(
    private val ingredientRepository: IngredientRepository
) {
    @Cacheable("ingredientNames")
    fun allIngredientNamesLowercased(): List<String> {
        return ingredientRepository.findAll()
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .map { it.lowercase() }
            .distinct()
    }

    fun suggest(
        query: String,
        limit: Int = 5,
        maxDistance: Int = 2
    ): DidYouMeanResponse {
        val q = query.trim().lowercase()
        if (q.isBlank()) return DidYouMeanResponse(query = query, suggestions = emptyList())

        // Small optimization: don't compare against extremely short/long candidates
        val candidates = allIngredientNamesLowercased()
            .asSequence()
            .filter { name -> kotlin.math.abs(name.length - q.length) <= max(2, maxDistance + 1) }
            .toList()

        val scored = candidates
            .map { candidate -> candidate to LevenshteinDistance.distance(q, candidate) }
            .filter { (_, dist) -> dist <= maxDistance }
            .sortedWith(compareBy({ it.second }, { it.first }))
            .take(limit)
            .map { it.first }

        return DidYouMeanResponse(query = query, suggestions = scored)
    }
}
