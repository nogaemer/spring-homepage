package de.nogaemer.springhomepage.main.units

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface UnitRepository : MongoRepository<IngredientUnit, String> {
    fun findById(id: ObjectId): MutableList<IngredientUnit>


}