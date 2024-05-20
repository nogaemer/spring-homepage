package de.nogaemer.springhomepage.user.dtos

data class UserDto(
    val id: Long,
    val name: String,
    val login: String,
    var token: String,
)