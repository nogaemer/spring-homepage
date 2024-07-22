package de.nogaemer.springhomepage.meals.import.backup

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.meals.models.DoubleSerializer
import de.nogaemer.springhomepage.meals.models.Ingredient
import de.nogaemer.springhomepage.meals.notes.Note
import de.nogaemer.springhomepage.meals.ratings.Rating
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference

@Document(collection = "meals")
@Data
@AllArgsConstructor
@NoArgsConstructor
data class BackupMealModel(
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

    val url: String = "",

    @NotNull
    var tags: List<String> = emptyList(),

    @JsonSerialize(using = DoubleSerializer::class)
    var rating: Double = 0.0
) {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: ObjectId? = null

    @DocumentReference
    var ratings: List<Rating> = emptyList()

    @DocumentReference
    var notes: List<Note> = emptyList()

    fun calculateRating(): Double {
        ratings.map { println(it.rating) }
        if (ratings.isEmpty()) return 0.0
        return ratings.map { it.rating }.average()
    }
}

