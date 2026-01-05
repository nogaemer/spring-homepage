package de.nogaemer.springhomepage.main.meals.dto

import de.nogaemer.springhomepage.main.ingredients.Ingredient
import de.nogaemer.springhomepage.main.units.IngredientUnitDto

data class MealIngredientDto(
    val ingredient: Ingredient,
    val amount: String,
    val unit: IngredientUnitDto
)