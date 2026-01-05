package de.nogaemer.springhomepage.main.tags

import org.springframework.data.mongodb.repository.MongoRepository

interface TagRepository : MongoRepository<Tag, String> {


}