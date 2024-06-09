package de.nogaemer.springhomepage.security.token

import de.nogaemer.springhomepage.user.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import java.util.Optional

interface TokenRepository : MongoRepository<Token, ObjectId> {

    @Query("{ 'user._id' : ?0, 'expired' : false, 'revoked' : false }")
    fun findAllValidTokenByUser(id: ObjectId): List<Token>

    @Query("{ 'user._id' : ?0 }")
    fun findAllTokenByUser(id: ObjectId): List<Token>

    fun findByToken(token: String): Token?

    fun findTokenByUserAndToken(user: User?, refreshToken: String): Token?


}