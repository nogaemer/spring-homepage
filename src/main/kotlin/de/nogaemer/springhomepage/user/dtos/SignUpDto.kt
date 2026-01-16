package de.nogaemer.springhomepage.user.dtos

import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor


/**
 * Data Transfer Object for user registration (sign up).
 *
 * Contains all required information to create a new user account.
 * Password is stored as CharArray for security reasons (see CredentialsDto).
 *
 * ## Security Note:
 * The password CharArray should be cleared after use to minimize
 * exposure in memory: `password.fill('0')`
 *
 * ## Equals/HashCode Implementation:
 * Custom implementations properly compare CharArray contents using
 * `contentEquals()` rather than reference equality.
 *
 * @property name User's display name
 * @property login User's unique login identifier (username or email)
 * @property password User's plaintext password as character array (will be encoded before storage)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
data class SignUpDto (
    val name: String,
    val login: String,
    val password: CharArray,
) {
    /**
     * Compares this SignUpDto with another object for equality.
     *
     * Uses content-based comparison for password array rather than reference equality.
     *
     * @param other Object to compare with
     * @return true if name, login, and password contents are equal
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignUpDto

        if (name != other.name) return false
        if (login != other.login) return false
        if (!password.contentEquals(other.password)) return false

        return true
    }

    /**
     * Generates hash code for this SignUpDto.
     *
     * Uses content-based hashing for password array to ensure
     * consistent hashing with equals() implementation.
     *
     * @return Hash code based on name, login, and password contents
     */
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + login.hashCode()
        result = 31 * result + password.contentHashCode()
        return result
    }
}