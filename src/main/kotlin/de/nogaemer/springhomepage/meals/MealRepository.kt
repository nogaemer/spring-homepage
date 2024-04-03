package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.meals.models.Meal
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface MealRepository : MongoRepository<Meal, ObjectId> {
    fun findByName(name: String?): Meal
}