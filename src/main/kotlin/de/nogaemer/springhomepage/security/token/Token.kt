package de.nogaemer.springhomepage.security.token

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.user.User
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document

/**
 * MongoDB entity representing a JWT authentication token.
 *
 * Stores JWT tokens in the database to track token lifecycle and enable revocation.
 * Each token is linked to a user and can be marked as expired or revoked.
 *
 * ## Token Lifecycle:
 * 1. **Creation**: Token is generated and saved with `revoked=false`, `expired=false`
 * 2. **Active**: Token can be used for authentication until it expires or is revoked
 * 3. **Revoked**: Token is marked `revoked=true` during logout (cannot be used anymore)
 * 4. **Expired**: Token is marked `expired=true` when JWT expiration time is reached
 * 5. **Cleanup**: Expired tokens are periodically deleted by scheduled task
 *
 * ## User Relationship:
 * The @DBRef annotation creates a reference to the User document in MongoDB.
 * This is a lazy-loaded relationship - the user is only fetched when accessed.
 *
 * **Important**: The user field is nullable to prevent NullPointerException during
 * token lookups and validations. Some operations may query tokens without needing
 * to load the full user object. Always perform null checks when accessing the user.
 *
 * @property token The actual JWT token string (unique identifier)
 * @property tokenType Type of token (currently only BEARER is supported)
 * @property revoked Whether token has been manually revoked (defaults to false)
 * @property expired Whether token has expired based on JWT expiration time (defaults to false)
 * @property user Reference to the user who owns this token (nullable, lazy-loaded via @DBRef)
 * @property id MongoDB ObjectId (auto-generated, serialized as string in JSON)
 */
@Document(collection = "tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
data class Token(

    val token: String,
    val tokenType: TokenType = TokenType.BEARER,
    var revoked: Boolean = false,
    var expired: Boolean = false,
    @DBRef
    var user: User? = null
){
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: ObjectId? = null
}
