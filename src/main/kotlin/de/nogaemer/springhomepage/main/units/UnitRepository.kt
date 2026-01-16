/**
 * MongoDB repository interface for ingredient unit persistence operations.
 *
 * Provides CRUD operations and custom queries for ingredient units.
 * Spring Data MongoDB automatically implements basic operations and
 * derives query implementations from method names.
 */
package de.nogaemer.springhomepage.main.units

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

/**
 * Repository for ingredient unit database operations.
 *
 * Extends [MongoRepository] to inherit standard CRUD methods (save, findById,
 * findAll, delete, etc.). String ID type allows MongoDB to handle ObjectId
 * conversion automatically.
 *
 * Custom query methods use derived query naming conventions from Spring Data.
 */
interface UnitRepository : MongoRepository<IngredientUnit, String> {
    
    /**
     * Finds units by ObjectId.
     *
     * Returns a list to match the return type, though typically only one unit
     * would match a unique ID.
     *
     * @param id The MongoDB ObjectId to search for
     * @return List containing the matching unit (empty if not found)
     */
    fun findById(id: ObjectId): MutableList<IngredientUnit>


}