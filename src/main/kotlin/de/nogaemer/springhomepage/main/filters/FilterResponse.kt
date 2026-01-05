package de.nogaemer.springhomepage.main.filters

import de.nogaemer.springhomepage.main.tags.Tag
import de.nogaemer.springhomepage.user.UserResponse


data class FilterResponse (
    val users: List<UserResponse>,
    val tags: List<Tag>
)