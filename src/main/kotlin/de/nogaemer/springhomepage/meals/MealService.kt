package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.meals.models.Meal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MealService {
    @Autowired
    private val repository: MealRepository? = null

    fun findAll(): List<Meal> {
        return repository!!.findAll()
    }

    fun findByName(name: String?): Meal {
        println(repository!!.findByName(name).toString())
        return repository!!.findByName(name)
    }

    fun create(meal: Meal): Meal {
        return repository!!.save(meal)
    }
}
