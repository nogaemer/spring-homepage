package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.exceptions.AlreadyReported
import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.meals.dto.MealDto
import de.nogaemer.springhomepage.meals.import.Chefkoch
import de.nogaemer.springhomepage.meals.models.Meal
import de.nogaemer.springhomepage.meals.models.MealImportMethod
import de.nogaemer.springhomepage.meals.ratings.RatingService
import de.nogaemer.springhomepage.meals.tags.TagService
import org.bson.types.ObjectId
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class MealService(
    val repository: MealRepository,
    val ratingService: RatingService,
    val tagService: TagService,
    val applicationContext: ApplicationContext
) {

    private fun self(): MealService = applicationContext.getBean(MealService::class.java)


    @Cacheable("allMeals")
    fun findAll(): List<Meal> {
        return repository.findAll()
    }

    @Cacheable("meals")
    fun findById(id: ObjectId): Meal {
        return repository.findById(id).orElseThrow { IllegalArgumentException("Meal with id $id not found") }
    }

    fun searchByName(name: String?): List<Meal>? {
        if (name == "") return self().findAll()
        return repository.searchByName(name)
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

    @Async
    fun importMealAsync(tag: MealImportMethod, url: String, save: Boolean = true): CompletableFuture<Meal> {
        return CompletableFuture.supplyAsync {
            when (tag) {
                MealImportMethod.CHEFKOCH -> {
                    if (!url.contains("chefkoch.de")) throw IllegalArgumentException("Url is not from Chefkoch")
                    val meal = Chefkoch().getMealFromUrl(url)

                    if (!save) return@supplyAsync meal

                    if (repository.countByUrl(url) >= 1)
                        throw AlreadyReported("Meal with url $url already exists", meal)

                    repository.save(meal)
                }
            }
        }
    }

    @Caching(evict = [
        CacheEvict(cacheNames = ["meals"], key = "#id"),
        CacheEvict(cacheNames = ["allMeals"], allEntries = true)
    ])
    fun deleteById(id: ObjectId) {
        val meal = self().findById(id)

        ratingService.deleteRatingsByMeal(meal)
        repository.deleteById(id)
    }

    @Caching(evict = [
        CacheEvict(cacheNames = ["meals"], key = "#id"),
        CacheEvict(cacheNames = ["allMeals"], allEntries = true)
    ])
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