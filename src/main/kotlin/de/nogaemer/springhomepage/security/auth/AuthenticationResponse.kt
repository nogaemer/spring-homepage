package de.nogaemer.springhomepage.security.auth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId

@Data
@AllArgsConstructor
@NoArgsConstructor
data class AuthenticationResponse(
    @JsonProperty("access_token")
    val accessToken: String? = null,

    @JsonProperty("refresh_token")
    val refreshToken: String? = null,

    @field:JsonSerialize(using = ToStringSerializer::class)
    val userId: ObjectId? = null
)
