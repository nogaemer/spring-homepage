package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.meals.models.Meal
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MealRepository : MongoRepository<Meal, ObjectId> {

    @Query("{ 'name' : { \$regex: ?0, \$options: 'i' } }")
    fun searchByName(name: String?): List<Meal>?

    fun findById(id: ObjectId?): Meal?

    fun findByName(name: String?): Meal?

    fun findByUrl(url: String?): Meal?

    @Query(value = " {'url': ?0} ", count = true)
    fun countByUrl(url: String?): Int
}