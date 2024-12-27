package de.nogaemer.springhomepage.main.ratings

import de.nogaemer.springhomepage.main.notes.Note
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RatingRepository: MongoRepository<Rating, ObjectId> {

    fun findByUserIdAndMealId(userId: ObjectId, mealId: ObjectId): Rating?

    fun deleteAllByMealId(mealId: ObjectId)

    fun findByMealId(mealId: ObjectId):List<Rating>
}
