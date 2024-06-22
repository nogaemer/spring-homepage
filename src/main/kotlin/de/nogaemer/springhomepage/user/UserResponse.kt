package de.nogaemer.springhomepage.user

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.bson.types.ObjectId

data class UserResponse(
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: ObjectId,
    val login: String,
    val name: String,
    val role: Role,
)