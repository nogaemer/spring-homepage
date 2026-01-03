package de.nogaemer.springhomepage.main.meals.dto

import de.nogaemer.springhomepage.main.meals.ingredients.Ingredient
import de.nogaemer.springhomepage.main.meals.units.IngredientUnitDto

data class MealIngredientDto(
    val ingredient: Ingredient,
    val amount: String,
    // client sends only the unit id (hex string), e.g. "69459a8553d53f7c239bf965"
    val unit: IngredientUnitDto
)