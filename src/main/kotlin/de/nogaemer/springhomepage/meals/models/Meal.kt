package de.nogaemer.springhomepage.meals.models

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.meals.ratings.Rating
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
    val imageUrls: List<String>,
    val difficulty: String,
    val time: Long,
    val portions: Int,
    val calories: Int,
    val url: String = "",
    var rating: Double = 0.0
){
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    private var id: ObjectId? = null

    @DocumentReference
    var ratings: List<Rating> = emptyList()

    fun calculateRating(): Double {
        ratings.map { println(it.rating) }
        if (ratings.isEmpty()) return 0.0
        return ratings.map { it.rating }.average()
    }
}

