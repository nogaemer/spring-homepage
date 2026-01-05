package de.nogaemer.springhomepage.main.meals

import de.nogaemer.springhomepage.exceptions.AlreadyReported
import de.nogaemer.springhomepage.exceptions.IdNotFoundException
import de.nogaemer.springhomepage.main.meals.models.Meal
import de.nogaemer.springhomepage.user.Role
import de.nogaemer.springhomepage.user.UserService
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.repository.MongoRepository

abstract class BaseService<T : EntityWithMealId, ID : Any>(
    private val repository: MongoRepository<T, ID>,
    private val mealRepository: MealRepository,
    private val mongoTemplate: MongoTemplate,
    private val userService: UserService,

    @Autowired private val cacheManager: CacheManager
) {

    open fun create(response: T): T {
        val meal = mealRepository.findById(response.mealId).orElseThrow { throw IdNotFoundException("Meal not found") }

        if (response.userId == null) response.userId = userService.getCurrentUser().id

        findByUserId(response.userId!!, meal.id!!)?.let {
            if (it.mealId == response.mealId) throw AlreadyReported(
                "User already submitted this type of entity for this meal",
                it
            )
        }

        val savedEntity = repository.save(response)

        mongoTemplate.update(Meal::class.java).matching(Criteria.where("id").`is`(savedEntity.mealId))
            .apply(Update().push(entityFieldName, savedEntity)).first()

        cacheManager.getCache("meals")!!.put(savedEntity.mealId, meal)
        cacheManager.getCache("allMeals")!!.clear()

        return savedEntity
    }

    open fun delete(id: ID, entity: T): T {
        if (userService.getCurrentUser().id != entity.userId && userService.getCurrentUser().role != Role.ADMIN) throw RuntimeException(
            "You are not allowed to delete this entity"
        )

        mongoTemplate.update(Meal::class.java).matching(Criteria.where("id").`is`(entity.mealId))
            .apply(Update().pull(entityFieldName, entity)).first()

        repository.deleteById(id)

        cacheManager.getCache("meals")!!.evict(entity.mealId)
        cacheManager.getCache("allMeals")!!.clear()

        return entity
    }

    abstract fun findByUserId(userId: ObjectId, mealId: ObjectId): T?
    abstract val entityFieldName: String
}

interface EntityWithMealId {
    var userId: ObjectId?
    val mealId: ObjectId
}
//
//@Component
//class CachePrinter(@Autowired private val cacheManager: CacheManager) {
//
//    fun printCache() {
//        val cacheNames = cacheManager.cacheNames
//        println("Cache Content:")
//        cacheNames.forEach { cacheName ->
//            println("Cache Name: $cacheName")
//            val cache = cacheManager.getCache(cacheName)
//            cache?.nativeCache?.let { nativeCache ->
//                if (nativeCache is com.github.benmanes.caffeine.cache.Cache<*, *>) {
//                    nativeCache.asMap().forEach { (key, value) ->
//                        println("Key: $key, Value: $value")
//                    }
//                } else {
//                    println("Cache type ${nativeCache.javaClass.name} not supported for direct printing")
//                }
//            }
//        }
//    }
//}