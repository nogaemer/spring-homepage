package de.nogaemer.springhomepage.main.meals.dto

import com.fasterxml.jackson.annotation.JsonInclude
import de.nogaemer.springhomepage.main.images.Image

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MealCardDto(
    val id: String,
    val name: String,
    val rating: Double,
    val time: Long,
    val difficulty: String,
    val images: List<Image>? = null,
    val instructions: List<String>? = null,

    // Optional fields for specific filters
    val relevanceScore: Double? = null,
    val matchingRatio: Double? = null,
    val matchingIngredients: List<SimpleMealIngredientDto>? = null,
    val ingredients: List<SimpleMealIngredientDto>? = null,
    val userRatings: List<Any>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SimpleMealIngredientDto(
    val name: String,
    val amount: String,
    val ingredientName: String? = null,
    val ingredientCategory: String? = null,
    val unitAbbreviation: String? = null,
    val unitFullName: String? = null
)

