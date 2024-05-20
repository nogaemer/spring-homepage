package de.nogaemer.springhomepage.meals.ratings

import de.nogaemer.springhomepage.exceptions.AlreadyReported
import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.meals.MealRepository
import de.nogaemer.springhomepage.meals.models.Meal
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service


@Service
class RatingService {

    @Autowired
    private val repository: RatingRepository? = null

    @Autowired
    private val mealRepository: MealRepository? = null

    @Autowired
    private val mongoTemplate: MongoTemplate? = null

    fun findAll(): List<Rating> {
        return repository!!.findAll()
    }

    fun create(response: Rating): Rating {
        mealRepository!!.findById(response.mealId)
            .orElseThrow { throw IdNotFoundException("Meal not found") }

        repository!!.findByUserId(response.userId!!).let {
            it ?: return@let
            if (it.mealId == response.mealId) throw AlreadyReported("User already rated this meal", response)
        }

        val rating = repository.insert(response)

        mongoTemplate!!.update(Meal::class.java)
            .matching(Criteria.where("id").`is`(rating.mealId))
            .apply(Update().push("ratings").value(rating))
            .first()

        return rating
    }

    fun delete(id: ObjectId) {

        val rating = repository!!.findById(id)
            .orElseThrow { throw IdNotFoundException("Rating not found") }

        mongoTemplate!!.update(Meal::class.java)
            .matching(Criteria.where("id").`is`(rating.mealId))
            .apply(Update().pull("ratings", rating))
            .first()

        repository.deleteById(id)
    }

}
