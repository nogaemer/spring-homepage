package de.nogaemer.springhomepage.security.auth

import de.nogaemer.springhomepage.user.Role
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
data class RegisterRequest(
    val name: String,
    val login: String,
    val password: String,
    val role: Role
)
