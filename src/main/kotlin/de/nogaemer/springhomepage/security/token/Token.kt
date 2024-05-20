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

@Document(collection = "tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
data class Token(
    val token: String,
    val tokenType: TokenType = TokenType.BEARER,
    var revoked: Boolean,
    var expired: Boolean,
    @DBRef
    var user: User
){
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: ObjectId? = null
}
