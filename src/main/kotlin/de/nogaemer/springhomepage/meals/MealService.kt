package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.exceptions.AlreadyReported
import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.meals.import.Chefkoch
import de.nogaemer.springhomepage.meals.models.Meal
import de.nogaemer.springhomepage.meals.models.MealImportMethod
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class MealService {
    @Autowired
    private val repository: MealRepository? = null

    fun findAll(): List<Meal> {
        return repository!!.findAll()
    }

    fun findByName(name: String?): List<Meal>? {
        name ?: return findAll()

        return repository!!.findByName(name)
    }

    fun findById(id: ObjectId): Meal {
        return repository!!.findById(id).getOrNull()
            ?: throw IdNotFoundException("Meal with id $id not found")
    }

    fun create(meal: Meal): Meal {
        return repository!!.save(meal)
    }

    fun importMeal(tag: MealImportMethod, url: String): Meal {
        when (tag) {
            MealImportMethod.CHEFKOCH -> {
                if (!url.contains("chefkoch.de")) throw IllegalArgumentException("Url is not from Chefkoch")
                val meal = Chefkoch().getMealFromUrl(url)

                if (repository!!.countByUrl(url) >= 1)
                    throw AlreadyReported("Meal with url $url already exists", meal)

                return repository!!.save(meal)
            }
        }
    }
}
