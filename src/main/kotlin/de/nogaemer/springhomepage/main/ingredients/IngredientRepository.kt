package de.nogaemer.springhomepage.main.ingredients

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface IngredientRepository : MongoRepository<Ingredient, String> {
    fun findById(id: ObjectId): MutableList<Ingredient>
}

