package de.nogaemer.springhomepage.main.filters

import de.nogaemer.springhomepage.user.UserResponse


data class FilterResponse (
    val users: List<UserResponse>,
    val sortParameters: List<SortParameter>,
)

data class SortParameter(
    val id: String,
    val name: String,
    val selected: Boolean
)