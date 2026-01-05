package de.nogaemer.springhomepage.main.meals.dto

import com.fasterxml.jackson.annotation.JsonInclude
import org.bson.types.ObjectId

data class UnifiedMealSearchRequest(
    // name
    val name: String? = null,

    // tags/time
    val tagIds: List<String>? = null,
    val minTime: Long? = null,
    val maxTime: Long? = null,

    // ingredients (frontend sends names)
    val ingredients: List<ObjectId>? = null,
    val minIngredientMatch: Double? = null, // e.g. 0.5

    // ratings filter (optional)
    val userIds: List<String>? = null,      // filter ratings to these users
    val minUserRating: Double? = null,         // e.g. 4
    val requireUserRatingMatch: Boolean = false, // only keep meals with remaining ratings after filtering

    // sorting/paging
    val sortBy: SortBy = SortBy.RATING,
    val limit: Int = 30,
    val skip: Long = 0,
) {
    enum class SortBy {
        RELEVANCE,          // name token score
        RATING,             // meal.rating
        TIME_ASC,
        TIME_DESC,
        INGREDIENT_MATCH,   // matchingRatio
        USER_AVG_RATING     // averageUserRating
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UnifiedMealSearchResponse(
    val results: List<Any>, // will be MealCardDto; kept Any here to avoid import cycles in snippet
    val didYouMean: Map<String, List<String>>? = null,
    val resolvedIngredientIds: List<String>? = null
)
