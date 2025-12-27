package de.nogaemer.springhomepage.main.meals.units

import org.bson.types.ObjectId

data class IngredientUnitDto (
    var abbreviation: String? = null,
    var abbreviationPlural: String? = null,
    var fullName: String? = null,
    var fullNamePlural: String? = null,
    var countable: Boolean? = null,
    var category: String? = null,
    var description: String? = null,
    val id: ObjectId
)