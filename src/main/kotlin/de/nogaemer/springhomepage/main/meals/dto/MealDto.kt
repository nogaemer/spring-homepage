package de.nogaemer.springhomepage.main.meals.dto

import de.nogaemer.springhomepage.main.images.Image
import de.nogaemer.springhomepage.main.tags.Tag
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.jetbrains.annotations.NotNull
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Collections.emptyList

@Document(collection = "meals")
@Data
@AllArgsConstructor
@NoArgsConstructor
data class MealDto(
    @NotNull
    val name: String,

    val ingredients: List<MealIngredientDto>,

    val instructions: List<String>,

    val images: List<Image>,

    @NotNull
    val difficulty: String,

    val time: Long,

    val portions: Int,

    val calories: Int,

    @NotNull
    var tags: MutableList<Tag> = emptyList()
)

