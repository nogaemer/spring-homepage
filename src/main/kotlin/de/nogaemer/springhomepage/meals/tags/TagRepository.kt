package de.nogaemer.springhomepage.meals.tags

import org.springframework.data.mongodb.repository.MongoRepository

interface TagRepository : MongoRepository<Tag, String> {


}