package de.nogaemer.springhomepage.meals.dto

import de.nogaemer.springhomepage.meals.models.Ingredient
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.jetbrains.annotations.NotNull
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "meals")
@Data
@AllArgsConstructor
@NoArgsConstructor
data class MealDto(
    @NotNull
    val name: String,

    val ingredients: List<Ingredient>,

    val instructions: List<String>,

    val imageSrc: List<String>,

    val imageSrcSet: List<String>,

    @NotNull
    val difficulty: String,

    val time: Long,

    val portions: Int,

    val calories: Int,

    @NotNull
    var tags: List<String> = emptyList()
)

