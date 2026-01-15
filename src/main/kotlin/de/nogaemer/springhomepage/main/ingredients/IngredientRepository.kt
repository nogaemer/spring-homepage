/**
 * Spring Data MongoDB repository for Ingredient entities.
 *
 * Provides basic CRUD operations and custom queries for ingredient data access.
 */
package de.nogaemer.springhomepage.main.ingredients

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

/**
 * Repository interface for ingredient database operations.
 *
 * Extends MongoRepository to provide standard CRUD operations plus custom queries.
 */
interface IngredientRepository : MongoRepository<Ingredient, String> {
    /**
     * Finds ingredients by ObjectId.
     *
     * @param id The MongoDB ObjectId to search for
     * @return List of matching ingredients (typically 0 or 1 result)
     */
    fun findById(id: ObjectId): MutableList<Ingredient>
}

