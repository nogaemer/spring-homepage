package de.nogaemer.springhomepage.security.token

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import java.util.Optional

interface TokenRepository : MongoRepository<Token, ObjectId> {

    @Query("{ 'user._id' : ?0, 'expired' : false, 'revoked' : false }")
    fun findAllValidTokenByUser(id: ObjectId): List<Token>

    fun findByToken(token: String): Token?
}