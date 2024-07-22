package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.exceptions.AlreadyReported
import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.meals.dto.MealDto
import de.nogaemer.springhomepage.meals.import.Chefkoch
import de.nogaemer.springhomepage.meals.models.Meal
import de.nogaemer.springhomepage.meals.models.MealImportMethod
import de.nogaemer.springhomepage.meals.ratings.RatingService
import de.nogaemer.springhomepage.meals.tags.Tag
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import de.nogaemer.springhomepage.meals.tags.TagService
import kotlin.jvm.optionals.getOrNull

@Service
class MealService(
    val repository: MealRepository,
    val ratingService: RatingService,
    val tagService: TagService
) {

    fun findAll(): List<Meal> {
        return repository.findAll()
    }

    fun searchByName(name: String?): List<Meal>? {
        if (name == "") return findAll()

        return repository.searchByName(name)
    }

    fun findById(id: ObjectId): Meal {
        return repository.findById(id).getOrNull()
            ?: throw IdNotFoundException("Meal with id $id not found")
    }

    fun create(meal: MealDto): Meal {

        repository.findByName(meal.name)?.let {
            throw AlreadyReported("Meal with name ${meal.name} already exists", meal)
        }

        val tags = tagService.stringToTags(meal.tags)

        val newMeal = Meal(
            name = meal.name,
            ingredients = meal.ingredients,
            instructions = meal.instructions,
            imageSrc = meal.imageSrc,
            imageSrcSet = meal.imageSrcSet,
            difficulty = meal.difficulty,
            time = meal.time,
            portions = meal.portions,
            calories = meal.calories,
            tags = mutableListOf()
        )

        val returnMeal = repository.save(newMeal)

        tagService.addTagsToMeal(tags, returnMeal)

        return returnMeal
    }

    fun create(meal: Meal): Meal {
        repository.findByName(meal.name)?.let {
            throw AlreadyReported("Meal with name ${meal.name} already exists", meal)
        }

        return repository.save(meal)
    }

    fun importMeal(tag: MealImportMethod, url: String, save: Boolean = true): Meal {
        when (tag) {
            MealImportMethod.CHEFKOCH -> {
                if (!url.contains("chefkoch.de")) throw IllegalArgumentException("Url is not from Chefkoch")
                val meal = Chefkoch().getMealFromUrl(url)

                if (!save) return meal

                if (repository.countByUrl(url) >= 1)
                    throw AlreadyReported("Meal with url $url already exists", meal)

                return repository.save(meal)
            }
        }
    }

    fun deleteById(id: ObjectId) {
        val meal = findById(id)

        // Assuming you have a service for handling ratings
        ratingService.deleteRatingsByMeal(meal)
        repository.deleteById(id)
    }

    fun update(id: ObjectId, meal: MealDto): Meal {

        val originalMeal = repository.findById(id).orElseThrow {
            IdNotFoundException("Meal with id $id not found")
        }

        val tags = tagService.updateMealTags(originalMeal, meal.tags)

        val updatedMeal = originalMeal.copy(
            name = meal.name,
            ingredients = meal.ingredients,
            instructions = meal.instructions,
            imageSrc = meal.imageSrc,
            imageSrcSet = meal.imageSrcSet,
            difficulty = meal.difficulty,
            time = meal.time,
            portions = meal.portions,
            calories = meal.calories,
            tags = tags,
            url = originalMeal.url,
            rating = originalMeal.rating
        ).apply {
            this.id = originalMeal.id
            this.ratings = originalMeal.ratings
            this.notes = originalMeal.notes
        }

        return repository.save(updatedMeal)
    }
}
