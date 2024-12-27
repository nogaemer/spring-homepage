package de.nogaemer.springhomepage.main.meals.tags

import de.nogaemer.springhomepage.main.meals.MealRepository
import de.nogaemer.springhomepage.main.meals.models.Meal
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service

@Service
class TagService(
    val tagRepository: TagRepository,
    val mealRepository: MealRepository,
    val mongoTemplate: MongoTemplate
) {

    fun saveTag(tag: Tag): Tag {
        return tagRepository.save(tag)
    }

    fun removeTag(tag: Tag) {
        tagRepository.delete(tag)
    }

    fun stringToTags(tags: List<String>): MutableList<Tag> {
        val tagList = mutableListOf<Tag>()

        tags.forEach {
            val formatedTadAsString = it.lowercase().trimStart()

            val tag = tagRepository.findById(formatedTadAsString)

            if (tag.isEmpty) {
                tagList.add(
                    saveTag(
                        Tag(
                            id = formatedTadAsString,
                            name = it.trimStart()
                        )
                    )
                )
            } else {
                tagList.add(tag.get())
            }
        }

        return tagList
    }

    fun addStringsAsTagsToMeal(tags: List<String>, meal: Meal): MutableList<Tag> {
        val tagList = stringToTags(tags)

        tagList.forEach { tag ->
            addTagToMeal(tag, meal)
        }

        return tagList
    }

    fun addTagToMeal(tag: Tag, meal: Meal): Tag {
        if (tag.meals.contains(meal.id!!)) {
            return tag
        }

        tag.meals.add(meal.id!!)

        saveTag(tag)

        mongoTemplate.update(Meal::class.java)
            .matching(Criteria.where("id").`is`(meal.id))
            .apply(Update().push("tags", tag))
            .first()

        return tag
    }

    fun addTagsToMeal(tags: List<Tag>, meal: Meal) {
        tags.forEach { tag ->
            addTagToMeal(tag, meal)
        }
    }

    fun removeTagFromMeal(tag: Tag, meal: Meal): Tag {
        if (!tag.meals.contains(meal.id!!)) {
            return tag
        }

        tag.meals.remove(meal.id!!)

        mongoTemplate.update(Meal::class.java)
            .matching(Criteria.where("id").`is`(meal.id))
            .apply(Update().pull("tags", tag))
            .first()

        return saveTag(tag)
    }

    fun updateMealTags(originalMeal: Meal, tags: MutableList<Tag>) {

        val originalTags = originalMeal.tags

        originalTags.forEach { tag ->
            if (!tags.contains(tag)) {
                val addedTag = removeTagFromMeal(tag, originalMeal)
                if (addedTag.meals.size == 0) {
                    removeTag(tag)
                }
            }
        }

        tags.forEach { tag ->
            if (!originalTags.contains(tag)) {
                addTagToMeal(tag, originalMeal)
            }
        }
    }

    fun updateMealTags(originalMeal: Meal, tags: List<String>): MutableList<Tag> {
        val tagList = stringToTags(tags)
        updateMealTags(originalMeal, tagList)

        return tagList
    }

}