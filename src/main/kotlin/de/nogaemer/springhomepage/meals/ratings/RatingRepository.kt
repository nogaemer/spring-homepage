package de.nogaemer.springhomepage.meals.ratings

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RatingRepository: MongoRepository<Rating, ObjectId> {

    fun findByUserId(userId: ObjectId): Rating?

    fun deleteAllByMealId(mealId: ObjectId)
}
