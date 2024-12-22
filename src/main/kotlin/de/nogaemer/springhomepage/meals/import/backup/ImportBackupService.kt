package de.nogaemer.springhomepage.meals.import.backup

import de.nogaemer.springhomepage.meals.MealService
import de.nogaemer.springhomepage.meals.models.Meal
import de.nogaemer.springhomepage.meals.notes.NoteService
import de.nogaemer.springhomepage.meals.ratings.RatingService
import de.nogaemer.springhomepage.meals.tags.TagService
import okhttp3.internal.wait
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service

@Service
class ImportBackupService(
    val mealService: MealService,
    val noteService: NoteService,
    val ratingService: RatingService,
    val tagService: TagService
) {

    fun import(meals: List<BackupMealModel>): List<Meal> {
        val mealList = mutableListOf<Meal>()

        meals.forEach {
            val tags = tagService.stringToTags(it.tags)

            val meal = mealService.create(
                Meal(
                    name = it.name,
                    ingredients = it.ingredients,
                    instructions = it.instructions,
                    images = it.images,
                    difficulty = it.difficulty,
                    time = it.time,
                    portions = it.portions,
                    calories = it.calories,
                    url = it.url,
                    rating = it.rating
                )
            )

            it.notes.forEach { note ->
                note.mealId = meal.id!!
                noteService.create(note)
            }

            it.ratings.forEach { rating ->
                rating.mealId = meal.id!!
                ratingService.create(rating)
            }

            tags.forEach { tag ->
                tagService.addTagToMeal(tag, meal)
            }

            mealList.add(meal)
        }

        return mealList

    }


}
