package de.nogaemer.springhomepage.meals.notes

import de.nogaemer.springhomepage.user.UserResponse

data class NoteResponse(
    val note: Note,
    val user: UserResponse
)