package de.nogaemer.springhomepage.meals.filters

import de.nogaemer.springhomepage.meals.tags.Tag
import de.nogaemer.springhomepage.user.UserResponse


data class FilterResponse (
    val users: List<UserResponse>,
    val tags: List<Tag>
)