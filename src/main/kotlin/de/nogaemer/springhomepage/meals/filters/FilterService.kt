package de.nogaemer.springhomepage.meals.filters

import de.nogaemer.springhomepage.meals.MealRepository
import de.nogaemer.springhomepage.meals.tags.Tag
import de.nogaemer.springhomepage.meals.tags.TagRepository
import de.nogaemer.springhomepage.user.UserRepository
import de.nogaemer.springhomepage.user.UserResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class FilterService(
    val userRepository: UserRepository,
    val tagRepository: TagRepository
) {

    fun getFilters(): FilterResponse {
        val users = userRepository.findAll().map { UserResponse(it.id!!, it.name) }
        val tags = tagRepository.findAll() as List<Tag>

        return FilterResponse(users, tags)
    }
}
