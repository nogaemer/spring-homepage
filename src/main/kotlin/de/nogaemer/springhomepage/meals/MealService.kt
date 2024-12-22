package de.nogaemer.springhomepage.meals

import de.nogaemer.springhomepage.exceptions.AlreadyReported
import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.meals.dto.MealDto
import de.nogaemer.springhomepage.meals.import.Chefkoch
import de.nogaemer.springhomepage.meals.models.Meal
import de.nogaemer.springhomepage.meals.models.MealImportMethod
import de.nogaemer.springhomepage.meals.ratings.RatingService
import de.nogaemer.springhomepage.meals.tags.TagService
import org.bson.BsonNull
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class MealService(
    val repository: MealRepository,
    val ratingService: RatingService,
    val tagService: TagService,
    val applicationContext: ApplicationContext,
    private val mongoTemplate: MongoTemplate
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
            images = meal.images,
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
                    val meal = Chefkoch(tagService).getMealFromUrl(url)

                    if (!save) return@supplyAsync meal

                    if (repository.countByUrl(url) >= 1)
                        throw AlreadyReported("Meal with url $url already exists", meal)

                    repository.save(meal)
                }
            }
        }
    }

    @Caching(
        evict = [
            CacheEvict(cacheNames = ["meals"], key = "#id"),
            CacheEvict(cacheNames = ["allMeals"], allEntries = true)
        ]
    )
    fun deleteById(id: ObjectId) {
        val meal = self().findById(id)

        ratingService.deleteRatingsByMeal(meal)
        repository.deleteById(id)
    }

    @Caching(
        evict = [
            CacheEvict(cacheNames = ["meals"], key = "#id"),
            CacheEvict(cacheNames = ["allMeals"], allEntries = true)
        ]
    )
    fun update(id: ObjectId, meal: MealDto): Meal {

        val originalMeal = repository.findById(id).orElseThrow {
            IdNotFoundException("Meal with id $id not found")
        }

        val tags = tagService.updateMealTags(originalMeal, meal.tags)

        val updatedMeal = originalMeal.copy(
            name = meal.name,
            ingredients = meal.ingredients,
            instructions = meal.instructions,
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

    fun filterMeals(
        name: String?,
        _users: String?,
        _tags: String?,
        time: Int?
    ): List<Meal> {
        val desiredTags = _tags?.split(",") ?: emptyList()
        val desiredName = name ?: ""
        val minTime = 0
        val maxTime = time ?: 1000
        var userIds = emptyList<ObjectId>()

        try {
            userIds = _users?.split(",")?.map { user -> ObjectId(user) } ?: emptyList()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid userIds")
        }

        val tagsCriteria = if (desiredTags.isNotEmpty()) {
            Criteria.where("tags").`in`(desiredTags)
        } else {
            Criteria.where("tags").exists(true)
        }


        // Stage 1: Match name and time
        val matchNameAndTime = match(
            Criteria()
                .andOperator(
                    Criteria.where("name").regex(desiredName, "i"),
                    Criteria.where("time").gte(minTime).lte(maxTime),
                    tagsCriteria
                )
        )

        // Stage 2: Lookup ratings
        val lookupRatings = lookup("ratings", "_id", "mealId", "userRatings")

        // Stage 5: Add fields to filter userRatings by userIds
        val filterRatings = Document(
            "\$filter", Document()
                .append("input", "\$userRatings")
                .append("as", "rating")
                .append(
                    "cond", Document(
                        "\$in", listOf(
                            "\$\$rating.userId", userIds
                        )
                    )
                )
        )

        val filterUserRatingsByUserId = addFields().addFieldWithValue(
            "userRatings",
            filterRatings
        ).build()

        // Stage 6: Unwind userRatings
        val addFieldsOperation = addFields().addFieldWithValue(
            "averageUserRating",
            Document(
                "\$cond", Document()
                    .append("if", Document("\$eq", listOf(Document("\$size", "\$userRatings"), 0)))
                    .append("then", 0)
                    .append(
                        "else", Document(
                            "\$divide", listOf(
                                Document(
                                    "\$sum",
                                    Document(
                                        "\$map",
                                        Document("input", "\$userRatings")
                                            .append("as", "userRating")
                                            .append(
                                                "in",
                                                Document(
                                                    "\$cond",
                                                    Document(
                                                        "if",
                                                        Document("\$ne", listOf("\$\$userRating.rating", BsonNull()))
                                                    ).append("then", "\$\$userRating.rating")
                                                        .append("else", 0L)
                                                )
                                            )
                                    )
                                ),
                                Document("\$size", "\$userRatings")
                            )
                        )
                    )
            )
        ).build()

        val sortByAverageRating = if (userIds.isEmpty()) {
            sort(Sort.by(Sort.Order.desc("rating")))
        } else {
            sort(Sort.by(Sort.Order.desc("averageUserRating")))
        }

        // Define the aggregation pipeline
        val aggregation = newAggregation(
            matchNameAndTime,
            lookupRatings,
            filterUserRatingsByUserId,
            addFieldsOperation,
            sortByAverageRating
        )

        // Execute the aggregation
        val results: AggregationResults<Meal> = mongoTemplate.aggregate(aggregation, "meals", Meal::class.java)

        return results.mappedResults
    }
}