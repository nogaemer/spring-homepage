package de.nogaemer.springhomepage.main.filters

import de.nogaemer.springhomepage.main.meals.tags.Tag
import de.nogaemer.springhomepage.main.meals.tags.TagRepository
import de.nogaemer.springhomepage.user.UserRepository
import de.nogaemer.springhomepage.user.UserResponse
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
