package de.nogaemer.springhomepage.updatedb

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service

@Service
class UpdateService(
    private val mongoTemplate: MongoTemplate,
    val linkInMeal_11_16_2024: LinkInMeal_11_16_2024,
    val linkIngredientUnit_12_27_2025: LinkIngredientUnit_12_27_2025
) {
    fun update(updateId: Int): Any {
        when (updateId) {
            1 -> {
                return linkInMeal_11_16_2024.updateAll()
            }
            2 -> {
                return linkIngredientUnit_12_27_2025.updateAll()
            }
        }

        return Any()
    }
}