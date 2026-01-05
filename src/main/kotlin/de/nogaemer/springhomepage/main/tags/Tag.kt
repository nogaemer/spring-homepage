package de.nogaemer.springhomepage.main.tags

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import jakarta.validation.constraints.Pattern
import org.bson.types.ObjectId
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "tags")
data class Tag(
    @field:NotNull
    val name: String,

    @field:NotNull
    val type: String,

    @field:NotNull
    val description: String,

    @field:Pattern(regexp = "^#([A-Fa-f0-9]{6})$")
    @field:NotNull
    val color: String
) {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: ObjectId? = null
}