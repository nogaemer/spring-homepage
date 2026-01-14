package de.nogaemer.springhomepage.main.ingredients

import de.nogaemer.springhomepage.main.units.IngredientUnit
import org.bson.types.ObjectId

data class IngredientDto(
    var id: ObjectId,
    val name: String,
    val category: String,
    val unit: IngredientUnit? = null,
    val priority: Int? = null
)

