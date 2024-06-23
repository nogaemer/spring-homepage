package de.nogaemer.springhomepage.meals.ratings

import BaseService
import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.exceptions.NotFoundException
import de.nogaemer.springhomepage.meals.MealRepository
import de.nogaemer.springhomepage.meals.models.Meal
import de.nogaemer.springhomepage.meals.ratings.RatingService.RatingUpdateMethod.*
import de.nogaemer.springhomepage.user.UserRepository
import de.nogaemer.springhomepage.user.UserResponse
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service


@Service
class RatingService(
    val repository: RatingRepository,
    val mealRepository: MealRepository,
    val userRepository: UserRepository,
    val mongoTemplate: MongoTemplate,
) : BaseService<Rating, ObjectId>(repository, mealRepository, mongoTemplate) {

    override fun findByUserId(userId: ObjectId, mealId: ObjectId): Rating? {
        return repository.findByUserIdAndMealId(userId, mealId)
    }

    override fun getEntityFieldName(): String {
        return "ratings"
    }

    fun findAll(): List<Rating> {
        return repository.findAll()
    }

    fun getRatingsByMealId(mealId: ObjectId): List<RatingResponse> {
        val ratings = mutableListOf<RatingResponse>()

        repository.findByMealId(mealId).forEach {
            val user = userRepository.findById(it.userId) ?: throw IdNotFoundException("User not found")

            ratings.add(
                RatingResponse(
                    it,
                    UserResponse(
                        user.id!!,
                        user.login,
                        user.name,
                        user.role
                    )
                )
            )
        }
        return ratings
    }

    override fun create(response: Rating): Rating {
        val rating = super.create(response)

        val meal = mealRepository.findById(response.mealId)
            .orElseThrow { throw IdNotFoundException("Meal not found") }

        updateRatings(meal, rating)

        return rating
    }

    override fun delete(id: ObjectId): Rating {
        val rating = super.delete(id)

        val meal = mealRepository.findById(rating.mealId)
            .orElseThrow { throw IdNotFoundException("Meal not found") }

        updateRatings(meal, rating, DELETE)

        return rating
    }

    fun updateRatings(meal: Meal, rating: Rating, delete: RatingUpdateMethod = ADD, originalRating: Rating? = null) {
        var newAverageRating = 0.0

        when (delete) {
            ADD -> {
                newAverageRating =
                    (meal.ratings.sumOf { it.rating }/ (meal.ratings.size).toDouble())
            }

            UPDATE -> {
                newAverageRating =
                    (meal.ratings.sumOf { it.rating } - originalRating!!.rating+ rating.rating) / (meal.ratings.size).toDouble()
            }

            DELETE -> {
                if (meal.ratings.size > 1) {
                    newAverageRating =
                        (meal.ratings.sumOf { it.rating }/ (meal.ratings.size).toDouble())
                }
            }
        }

        mongoTemplate.updateMulti(
            Query.query(Criteria.where("id").`is`(rating.mealId)),
            Update().set("rating", newAverageRating),
            Meal::class.java
        )
    }

    fun deleteRatingsByMeal(meal: Meal) {
        repository.deleteAllByMealId(meal.id!!)
    }

    fun findByMealId(objectId: ObjectId): List<Rating> {
        return repository.findByMealId(objectId)
    }

    fun update(id: ObjectId, rating: Rating): Rating? {
        val originalRating = repository.findById(id).orElseThrow {
            IdNotFoundException("Meal with id $id not found")
        }

        updateRatings(mealRepository.findById(originalRating.mealId).orElseThrow {
            NotFoundException("Meal not found")
        }, rating, UPDATE, originalRating)

        originalRating.rating = rating.rating

        return repository.save(originalRating)
    }


    enum class RatingUpdateMethod {
        ADD,
        UPDATE,
        DELETE
    }
}


