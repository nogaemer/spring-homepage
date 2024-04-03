package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.meals.models.Meal
import de.nogaemer.springhomepage.meals.models.Rating
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
    private val mongoTemplate: MongoTemplate? = null

    fun findAll(): List<Rating> {
        return repository!!.findAll()
    }

    fun create(response: Rating): Rating {
        val rating = repository!!.insert(response)

        mongoTemplate!!.update(Meal::class.java)
            .matching(Criteria.where("id").`is`(rating.mealId))
            .apply(Update().push("ratings").value(rating))
            .first()

        return rating
    }

}
