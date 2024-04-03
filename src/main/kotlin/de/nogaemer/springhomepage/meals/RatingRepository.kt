package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.meals.models.Meal
import de.nogaemer.springhomepage.meals.models.Rating
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface RatingRepository: MongoRepository<Rating, ObjectId> {

    fun findByUserId(userId: ObjectId): Rating
}
