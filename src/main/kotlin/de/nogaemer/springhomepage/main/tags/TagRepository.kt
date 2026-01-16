/**
 * MongoDB repository interface for tag persistence operations.
 *
 * Provides CRUD operations for tags.
 * Spring Data MongoDB automatically implements basic operations from the interface.
 */
package de.nogaemer.springhomepage.main.tags

import org.springframework.data.mongodb.repository.MongoRepository

/**
 * Repository for tag database operations.
 *
 * Extends [MongoRepository] to inherit standard CRUD methods (save, findById,
 * findAll, delete, etc.). String ID type allows MongoDB to handle ObjectId
 * conversion automatically.
 *
 * Note: Custom search logic is implemented in [TagService] using MongoTemplate
 * aggregations rather than derived query methods.
 */
interface TagRepository : MongoRepository<Tag, String> {


}