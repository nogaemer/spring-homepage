package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.exceptions.AlreadyReported
import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.meals.import.Chefkoch
import de.nogaemer.springhomepage.meals.models.Meal
import de.nogaemer.springhomepage.meals.models.MealImportMethod
import de.nogaemer.springhomepage.meals.ratings.RatingService
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import kotlin.jvm.optionals.getOrNull

@Service
class MealService {
    @Autowired
    private val repository: MealRepository? = null

    @Autowired
    private val ratingService: RatingService? = null

    fun findAll(): List<Meal> {
        return repository!!.findAll()
    }

    fun searchByName(name: String?): List<Meal>? {
        if (name == "") return findAll()

        return repository!!.searchByName(name)
    }

    fun findById(id: ObjectId): Meal {
        return repository!!.findById(id).getOrNull()
            ?: throw IdNotFoundException("Meal with id $id not found")
    }

    fun create(meal: Meal): Meal {

        val existingMeal = repository!!.findByName(meal.name)
        if (existingMeal != null) {
            throw AlreadyReported("Meal with name ${meal.name} already exists", existingMeal)
        }

        return repository!!.save(meal)
    }

    fun importMeal(tag: MealImportMethod, url: String, save:Boolean = true): Meal {
        when (tag) {
            MealImportMethod.CHEFKOCH -> {
                if (!url.contains("chefkoch.de")) throw IllegalArgumentException("Url is not from Chefkoch")
                val meal = Chefkoch().getMealFromUrl(url)

                if (!save) return meal

                if (repository!!.countByUrl(url) >= 1)
                    throw AlreadyReported("Meal with url $url already exists", meal)

                return repository!!.save(meal)
            }
        }
    }

    fun deleteById(id: ObjectId) {
        val meal = findById(id)

        // Assuming you have a service for handling ratings
        ratingService!!.deleteRatingsByMeal(meal)

        repository!!.deleteById(id)
    }

    fun update(id: ObjectId, meal: Meal): Meal {

    }
}
