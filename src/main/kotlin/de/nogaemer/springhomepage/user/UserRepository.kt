package de.nogaemer.springhomepage.user

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface UserRepository : MongoRepository<User, ObjectId> {

    fun findByLogin(login: String?): Optional<User>

    fun findById(id: ObjectId?): User?
}