package de.nogaemer.springhomepage.main.ingredients

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.main.units.IngredientUnit
import org.bson.types.ObjectId
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference

@Document(collection = "ingredients")
data class Ingredient(
    @field:NotNull
    val name: String,

    @field:NotNull
    val category: String,

    @DocumentReference
    val unit: IngredientUnit? = null
) {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: ObjectId? = null
}

