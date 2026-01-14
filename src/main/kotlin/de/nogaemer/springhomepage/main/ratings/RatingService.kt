package de.nogaemer.springhomepage.main.ratings

import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.main.meals.MealRepository
import de.nogaemer.springhomepage.main.meals.models.Meal
import de.nogaemer.springhomepage.main.ratings.RatingService.RatingUpdateMethod.*
import de.nogaemer.springhomepage.user.UserService
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.ArrayOperators
import org.springframework.data.mongodb.core.aggregation.MergeOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service


@Service
class RatingService(
    val repository: RatingRepository,
    val mealRepository: MealRepository,
    val ratingRepository: RatingRepository,
    val mongoTemplate: MongoTemplate,
    val userService: UserService,
    @Autowired
    private val cacheManager: CacheManager
) : de.nogaemer.springhomepage.main.meals.BaseService<Rating, ObjectId>(
    repository,
    mealRepository,
    mongoTemplate,
    userService,
    cacheManager
) {

    override fun findByUserId(userId: ObjectId, mealId: ObjectId): Rating? {
        return repository.findByUserIdAndMealId(userId, mealId)
    }

    override val entityFieldName = "ratings"

    fun findAll(): List<Rating> {
        return repository.findAll()
    }


    fun getRatingsByMealId(mealId: ObjectId): RatingResponse {

        val stages = mutableListOf<AggregationOperation>()
        stages += Aggregation.match(Criteria.where("_id").`is`(mealId))
        stages += Aggregation.project("ratings", "_id").and("rating").`as`("mealRating")
        stages += Aggregation.lookup("ratings", "ratings", "_id", "rating")
        stages += Aggregation.unwind("rating")
        stages += Aggregation.lookup("users", "rating.userId", "_id", "user")
        stages += Aggregation.unwind("user", true)
        stages += Aggregation.group("_id").push("\$\$ROOT").`as`("ratings")
        stages += Aggregation.addFields().addField("mealRating")
            .withValue(ArrayOperators.ArrayElemAt.arrayOf("\$ratings.mealRating").elementAt(0))
            .build()

        val pipeline = Aggregation.newAggregation(*stages.toTypedArray())
        println("Aggregation pipeline: $pipeline")
        val results = mongoTemplate.aggregate(pipeline, "meals", RatingResponse::class.java).mappedResults

        return results[0]
    }

    override fun create(response: Rating): Rating {
        val rating = super.create(response)

        val mealRatings = ratingRepository.findByMealId(rating.mealId)
            .ifEmpty { throw IdNotFoundException("Meal not found") }

        updateRatings(mealRatings, rating, ADD)

        cacheManager.getCache("ratings")!!.evict(rating.mealId)
        return rating
    }

    fun delete(id: ObjectId): Rating {
        val rating = repository.findById(id)
            .orElseThrow { throw IdNotFoundException("Rating not found") }

        return delete(id, rating)
    }

    override fun delete(id: ObjectId, rating: Rating): Rating {
        val mealRatings = ratingRepository.findByMealId(rating.mealId)
            .ifEmpty { throw IdNotFoundException("Meal not found") }

        updateRatings(mealRatings, rating, DELETE)

        super.delete(id, rating)

        cacheManager.getCache("ratings")!!.evict(rating.mealId)
        return rating
    }

    fun syncAverageRatings(rating: Rating) {
        val stages = mutableListOf<AggregationOperation>()
        stages += Aggregation.match(Criteria.where("mealId").`is`(rating.mealId))
        stages += Aggregation.group("mealId").avg("rating").`as`("rating")
        stages += Aggregation.merge().into(
            MergeOperation.MergeOperationTarget.collection("meals")
        ).on("_id")
            .whenMatched(
                MergeOperation.WhenDocumentsMatch.updateWith(
                    Aggregation.newAggregation(
                        Aggregation.addFields().addFieldWithValue("rating", "\$\$new.rating").build()
                    )
                )
            ).whenNotMatched(MergeOperation.WhenDocumentsDontMatch.discardDocument()).build()

        val pipeline = Aggregation.newAggregation(*stages.toTypedArray())
        println("Aggregation pipeline: $pipeline")
    }

    fun updateRatings(
        ratings: List<Rating>,
        rating: Rating,
        method: RatingUpdateMethod = ADD,
        originalRating: Rating? = null
    ) {
        var newAverageRating = 0.0

        when (method) {
            ADD -> {
                newAverageRating =
                    (ratings.sumOf { it.rating } / (ratings.size).toDouble())
            }

            UPDATE -> {
                newAverageRating =
                    (ratings.sumOf { it.rating } - originalRating!!.rating + rating.rating) / (ratings.size).toDouble()
            }

            DELETE -> {
                if (ratings.size > 1) {
                    newAverageRating =
                        (ratings.sumOf { it.rating } / (ratings.size).toDouble())
                }
            }
        }

        mongoTemplate.updateMulti(
            Query.query(Criteria.where("id").`is`(rating.mealId)),
            Update().set("rating", newAverageRating),
            Meal::class.java
        )
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

        val mealRatings = ratingRepository.findByMealId(rating.mealId)
            .ifEmpty { throw IdNotFoundException("Meal not found") }

        updateRatings(mealRatings, rating, UPDATE, originalRating)

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


