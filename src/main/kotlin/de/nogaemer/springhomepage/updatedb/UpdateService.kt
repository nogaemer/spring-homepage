package de.nogaemer.springhomepage.updatedb

import de.nogaemer.springhomepage.meals.models.ImgLink
import de.nogaemer.springhomepage.meals.models.Meal
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service

@Service
class UpdateService(
    private val mongoTemplate: MongoTemplate,
    val linkInMeal_11_16_2024: LinkInMeal_11_16_2024
) {
    fun update(updateId: Int): Any {
        when (updateId) {
            1 -> {
                return linkInMeal_11_16_2024.updateAll()
            }
            2 -> {
                return Any()
            }
        }

        return Any()
    }
}