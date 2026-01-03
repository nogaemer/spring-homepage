package de.nogaemer.springhomepage.updatedb

import org.springframework.stereotype.Service

@Service
class UpdateService(
    val linkInMeal_11_16_2024: LinkInMeal_11_16_2024,
    val ingredientUnit_12_27_2025: IngredientUnit_12_27_2025,
    val ingredientUnitForIngredients_01_02_2026: IngredientUnitForIngredients_01_02_2026
) {
    fun update(updateId: Int): Any {
        when (updateId) {
            1 -> {
                return linkInMeal_11_16_2024.updateAll()
            }
            2 -> {
                return ingredientUnit_12_27_2025.updateAll()
            }
            3 -> {
                return ingredientUnitForIngredients_01_02_2026.updateAll()
            }
        }

        return Any()
    }
}