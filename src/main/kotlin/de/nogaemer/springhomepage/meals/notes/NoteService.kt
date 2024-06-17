package de.nogaemer.springhomepage.meals.notes

import BaseService
import de.nogaemer.springhomepage.meals.MealRepository
import de.nogaemer.springhomepage.meals.models.Meal
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service


@Service
class NoteService(
    val repository: NoteRepository,
    mealRepository: MealRepository,
    mongoTemplate: MongoTemplate,
) : BaseService<Note, ObjectId>(repository, mealRepository, mongoTemplate) {

    override fun findByUserId(userId: ObjectId): Note? {
        return repository.findByUserId(userId)
    }

    override fun getEntityFieldName(): String {
        return "notes"
    }

    fun findAll(): List<Note> {
        return repository.findAll()
    }

    override fun create(response: Note): Note {
        return super.create(response)
    }

    override fun delete(id: ObjectId): Note {
        return super.delete(id)
    }

    fun deleteNotesByMeal(meal: Meal) {
        repository.deleteAllByMealId(meal.id!!)
    }

    fun findByMealId(objectId: ObjectId): List<Note> {
        return repository.findByMealId(objectId)
    }

}
