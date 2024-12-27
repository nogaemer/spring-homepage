package de.nogaemer.springhomepage.main.meals.tags

import org.springframework.data.mongodb.repository.MongoRepository

interface TagRepository : MongoRepository<Tag, String> {


}