package de.nogaemer.springhomepage.main.notes

import de.nogaemer.springhomepage.main.meals.BaseService
import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.main.meals.MealRepository
import de.nogaemer.springhomepage.main.meals.models.Meal
import de.nogaemer.springhomepage.user.UserRepository
import de.nogaemer.springhomepage.user.UserResponse
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service


@Service
class NoteService(
    val repository: NoteRepository,
    mealRepository: MealRepository,
    val userRepository: UserRepository,
    mongoTemplate: MongoTemplate,
    @Autowired
    cacheManager: CacheManager
) : de.nogaemer.springhomepage.main.meals.BaseService<Note, ObjectId>(repository, mealRepository, mongoTemplate, cacheManager) {

    override fun findByUserId(userId: ObjectId, mealId: ObjectId): Note? {
        return repository.findByUserIdAndMealId(userId, mealId)
    }

    override val entityFieldName = "notes"

    fun findAll(): List<Note> {
        return repository.findAll()
    }

    fun getNotesByMealId(mealId: ObjectId): List<NoteResponse> {
        val ratings = mutableListOf<NoteResponse>()

        repository.findByMealId(mealId).forEach {
            val user = userRepository.findById(it.userId)?:
            throw IdNotFoundException("User not found")

            ratings.add(
                NoteResponse(
                    it,
                    UserResponse(
                        user.id!!,
                        user.login
                    )
                )
            )
        }
        return ratings
    }

    override fun create(response: Note): Note {
        return super.create(response)
    }

    fun delete(id: ObjectId): Note {
        val note = repository.findById(id)
            .orElseThrow { IdNotFoundException("Note not found") }

        return delete(id, note)
    }

    override fun delete(id: ObjectId, entity: Note): Note {
        return super.delete(id, entity)
    }

    fun deleteNotesByMeal(meal: Meal) {
        repository.deleteAllByMealId(meal.id!!)
    }

    fun findByMealId(objectId: ObjectId): List<Note> {
        return repository.findByMealId(objectId)
    }

}
