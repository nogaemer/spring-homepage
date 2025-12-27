package de.nogaemer.springhomepage.main.meals.units

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.bson.types.ObjectId
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "units")
data class IngredientUnit(
    @field:NotNull
    val abbreviation: String,

    @field:NotNull
    val abbreviationPlural: String,

    @field:NotNull
    val fullName: String,

    @field:NotNull
    val fullNamePlural: String,

    @field:NotNull
    val countable: Boolean,

    @field:NotNull
    val category: String,

    @field:NotNull
    val description: String
) {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: ObjectId? = null
}