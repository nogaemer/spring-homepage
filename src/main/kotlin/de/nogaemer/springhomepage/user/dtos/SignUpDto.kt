package de.nogaemer.springhomepage.user.dtos

import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
data class SignUpDto (
    val name: String,
    val login: String,
    val password: CharArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignUpDto

        if (name != other.name) return false
        if (login != other.login) return false
        if (!password.contentEquals(other.password)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + login.hashCode()
        result = 31 * result + password.contentHashCode()
        return result
    }
}