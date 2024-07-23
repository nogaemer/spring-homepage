package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.exceptions.AlreadyReported
import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.meals.models.Meal
import de.nogaemer.springhomepage.user.User
import org.bson.types.ObjectId
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.security.core.context.SecurityContextHolder

abstract class BaseService<T : EntityWithMealId, ID : Any>(
    private val repository: MongoRepository<T, ID>,
    private val mealRepository: MealRepository,
    private val mongoTemplate: MongoTemplate
) {

    open fun create(response: T): T {
        val meal = mealRepository.findById(response.mealId)
            .orElseThrow { throw IdNotFoundException("Meal not found") }

        if (response.userId == null)
            response.userId = getCurrentUserId()

        findByUserId(response.userId!!, meal.id!!)?.let {
            if (it.mealId == response.mealId)
                throw AlreadyReported("User already submitted this type of entity for this meal", response)
        }

        val savedEntity = repository.save(response)

        mongoTemplate.update(Meal::class.java)
            .matching(Criteria.where("id").`is`(savedEntity.mealId))
            .apply(Update().push(getEntityFieldName(), savedEntity))
            .first()

        return savedEntity
    }

    open fun delete(id: ID): T {
        val entity = repository.findById(id)
            .orElseThrow { throw IdNotFoundException("Entity not found") }

        mongoTemplate.update(Meal::class.java)
            .matching(Criteria.where("id").`is`(entity.mealId))
            .apply(Update().pull(getEntityFieldName(), entity))
            .first()

        repository.deleteById(id)

        return entity
    }

    abstract fun findByUserId(userId: ObjectId, mealId: ObjectId): T?
    abstract fun getEntityFieldName(): String
}

interface EntityWithMealId {
    var userId: ObjectId?
    val mealId: ObjectId
}

fun getCurrentUserId(): ObjectId? {
    val authentication = SecurityContextHolder.getContext().authentication
    if (authentication != null && authentication.isAuthenticated) {
        val userDetails = authentication.principal as? User
        return userDetails?.id// Ensure your UserDetails implementation has a userId field
    }
    return null
}