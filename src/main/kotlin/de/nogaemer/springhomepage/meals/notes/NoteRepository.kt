package de.nogaemer.springhomepage.meals.notes

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface NoteRepository: MongoRepository<Note, ObjectId> {

    fun findByUserId(userId: ObjectId): Note?

    fun deleteAllByMealId(mealId: ObjectId)

    fun findByMealId(mealId: ObjectId): List<Note>
}
