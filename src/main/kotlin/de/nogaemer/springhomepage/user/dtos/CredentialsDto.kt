package de.nogaemer.springhomepage.user.dtos

import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor


/**
 * Data Transfer Object for user login credentials.
 *
 * Contains authentication credentials submitted during login.
 * Password is stored as CharArray (rather than String) for security:
 * - Can be explicitly cleared from memory after use
 * - Not interned in String pool
 * - Reduces exposure in memory dumps
 *
 * ## Security Note:
 * Always clear the password array after use with `password.fill('0')`
 * or similar to minimize password exposure in memory.
 *
 * ## Equals/HashCode Implementation:
 * Custom implementations properly compare CharArray contents using
 * `contentEquals()` rather than reference equality.
 *
 * @property login User's login identifier (username or email)
 * @property password User's plaintext password as character array
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
data class CredentialsDto (
    val login: String,
    val password: CharArray
) {
    /**
     * Compares this CredentialsDto with another object for equality.
     *
     * Uses content-based comparison for password array rather than reference equality.
     *
     * @param other Object to compare with
     * @return true if login and password contents are equal
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CredentialsDto

        if (login != other.login) return false
        if (!password.contentEquals(other.password)) return false

        return true
    }

    /**
     * Generates hash code for this CredentialsDto.
     *
     * Uses content-based hashing for password array to ensure
     * consistent hashing with equals() implementation.
     *
     * @return Hash code based on login and password contents
     */
    override fun hashCode(): Int {
        var result = login.hashCode()
        result = 31 * result + password.contentHashCode()
        return result
    }
}