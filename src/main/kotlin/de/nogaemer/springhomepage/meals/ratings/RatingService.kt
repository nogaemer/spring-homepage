package de.nogaemer.springhomepage.meals.ratings

import de.nogaemer.springhomepage.meals.BaseService
import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.exceptions.NotFoundException
import de.nogaemer.springhomepage.meals.MealRepository
import de.nogaemer.springhomepage.meals.models.Meal
import de.nogaemer.springhomepage.meals.ratings.RatingService.RatingUpdateMethod.*
import de.nogaemer.springhomepage.user.UserRepository
import de.nogaemer.springhomepage.user.UserResponse
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
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
    @Autowired
    private val cacheManager: CacheManager
) : BaseService<Rating, ObjectId>(repository, mealRepository, mongoTemplate, cacheManager) {

    override fun findByUserId(userId: ObjectId, mealId: ObjectId): Rating? {
        return repository.findByUserIdAndMealId(userId, mealId)
    }

    override val entityFieldName = "ratings"

    fun findAll(): List<Rating> {
        return repository.findAll()
    }


    @Cacheable("ratings")
    fun getRatingsByMealId(mealId: ObjectId): List<RatingResponse> {
        val ratings = mutableListOf<RatingResponse>()

        repository.findByMealId(mealId).forEach {
            val user = userRepository.findById(it.userId) ?: throw IdNotFoundException("User not found")

            ratings.add(
                RatingResponse(
                    it,
                    UserResponse(
                        user.id!!,
                        user.login
                    ),
                    mealRepository.findById(it.mealId).orElseThrow { NotFoundException("Meal not found") }.rating
                )
            )
        }
        return ratings
    }

    override fun create(response: Rating): Rating {
        val rating = super.create(response)

        val meal = mealRepository.findById(response.mealId)
            .orElseThrow { throw IdNotFoundException("Meal not found") }

        updateRatings(meal, rating, ADD)

        cacheManager.getCache("ratings")!!.evict(rating.mealId)
        return rating
    }

    fun delete(id: ObjectId): Rating{
        val rating = repository.findById(id)
            .orElseThrow { throw IdNotFoundException("Rating not found") }

        return delete(id, rating)
    }

    override fun delete(id: ObjectId, rating: Rating): Rating {
        val meal = mealRepository.findById(rating.mealId)
            .orElseThrow { throw IdNotFoundException("Meal not found") }

        updateRatings(meal, rating, DELETE)

        super.delete(id, rating)

        cacheManager.getCache("ratings")!!.evict(rating.mealId)
        return rating
    }

    fun updateRatings(meal: Meal, rating: Rating, method: RatingUpdateMethod = ADD, originalRating: Rating? = null) {
        var newAverageRating = 0.0

        when (method) {
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

        meal.rating = newAverageRating
        cacheManager.getCache("allMeals")!!.clear()
    }

    fun deleteRatingsByMeal(meal: Meal) {
        repository.deleteAllByMealId(meal.id!!)
    }

    fun findByMealId(objectId: ObjectId): List<Rating> {
        return repository.findByMealId(objectId)
    }


    fun update(id: ObjectId, rating: Rating): Rating? {
        val originalRating = repository.findById(id).orElseThrow {
            IdNotFoundException("Rating with id $id not found")
        }

        updateRatings(mealRepository.findById(originalRating.mealId).orElseThrow {
            NotFoundException("Meal not found")
        }, rating, UPDATE, originalRating)

        originalRating.rating = rating.rating


        cacheManager.getCache("meals")!!.evict(originalRating.mealId)
        cacheManager.getCache("ratings")!!.evict(originalRating.mealId)

        return repository.save(originalRating)
    }


    enum class RatingUpdateMethod {
        ADD,
        UPDATE,
        DELETE
    }
}


