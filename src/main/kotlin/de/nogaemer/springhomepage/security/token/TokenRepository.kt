package de.nogaemer.springhomepage.security.token

import de.nogaemer.springhomepage.user.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import java.util.Optional

/**
 * MongoDB repository for Token entity operations.
 *
 * Provides methods for querying, saving, and deleting JWT tokens stored in the database.
 * Supports token validation, revocation tracking, and user-token relationship queries.
 *
 * @see Token
 */
interface TokenRepository : MongoRepository<Token, ObjectId> {

    /**
     * Finds all valid (non-expired, non-revoked) tokens for a specific user.
     *
     * Used to:
     * - Check active sessions for a user
     * - Revoke all user tokens during security events
     * - List active authentication sessions
     *
     * @param id User's MongoDB ObjectId
     * @return List of valid tokens (may be empty)
     */
    @Query("{ 'user._id' : ?0, 'expired' : false, 'revoked' : false }")
    fun findAllValidTokenByUser(id: ObjectId): List<Token>

    /**
     * Finds all tokens (valid or invalid) for a specific user.
     *
     * Includes expired and revoked tokens. Useful for:
     * - Audit trails
     * - Token history analysis
     * - Cleanup operations
     *
     * @param id User's MongoDB ObjectId
     * @return List of all tokens for the user (may be empty)
     */
    @Query("{ 'user._id' : ?0 }")
    fun findAllTokenByUser(id: ObjectId): List<Token>

    /**
     * Finds a token by its JWT string value.
     *
     * Used during:
     * - Token validation in authentication filter
     * - Token revocation during logout
     * - Refresh token operations
     * - Duplicate token detection during token generation
     *
     * @param token JWT token string
     * @return Token entity or null if not found
     */
    fun findByToken(token: String): Token?

    /**
     * Finds a specific token for a user by token string.
     *
     * Used to validate that a token belongs to a specific user,
     * particularly during refresh token operations.
     *
     * @param user User entity (nullable for flexible queries)
     * @param refreshToken JWT token string to search for
     * @return Token entity or null if not found
     */
    fun findTokenByUserAndToken(user: User?, refreshToken: String): Token?


}