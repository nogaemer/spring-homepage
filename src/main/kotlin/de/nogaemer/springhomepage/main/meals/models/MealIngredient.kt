package de.nogaemer.springhomepage.main.meals.models

import de.nogaemer.springhomepage.main.meals.units.IngredientUnit
import org.springframework.data.mongodb.core.mapping.DocumentReference

data class MealIngredient(
    val name: String = "",
    val amount: String = "",

    @DocumentReference
    val unit: IngredientUnit
)