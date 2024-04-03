package de.nogaemer.springhomepage.meals.models

import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference

@Document(collection = "meals")
@Data
@AllArgsConstructor
@NoArgsConstructor
data class Meal(
    val name: String,
    val ingredients: List<Ingredient>,
    val instructions: List<String>,
    val tags: List<String>,
    val imageUrl: List<String>,
    val difficulty: Int,
    val time: Int,
    val portions: Int,
    val calories: Int,
){
    @Id
    private var id: ObjectId? = null

    @DocumentReference
    var ratings: List<Rating> = emptyList()
}

