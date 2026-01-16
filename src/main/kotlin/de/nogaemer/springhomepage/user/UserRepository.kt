package de.nogaemer.springhomepage.user

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*


/**
 * MongoDB repository for User entity operations.
 *
 * Provides methods for querying, saving, and deleting users from the MongoDB users collection.
 * Extends Spring Data MongoRepository for standard CRUD operations.
 *
 * Custom query methods support user lookup by login identifier and ObjectId.
 *
 * @see User
 */
@Repository
interface UserRepository : MongoRepository<User, ObjectId> {

    /**
     * Finds a user by their unique login identifier (username or email).
     *
     * Used during:
     * - User authentication (login)
     * - OAuth2 user lookup
     * - User registration (checking for duplicates)
     *
     * @param login User's login identifier (username or email)
     * @return Optional containing User if found, empty otherwise
     */
    fun findByLogin(login: String?): Optional<User>

    /**
     * Finds a user by their MongoDB ObjectId.
     *
     * Note: This method returns User directly (nullable) rather than Optional.
     *
     * @param id User's MongoDB ObjectId
     * @return User entity or null if not found
     */
    fun findById(id: ObjectId?): User?
}