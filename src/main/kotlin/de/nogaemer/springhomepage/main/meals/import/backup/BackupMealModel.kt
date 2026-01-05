package de.nogaemer.springhomepage.main.meals.import.backup

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.main.images.Image
import de.nogaemer.springhomepage.main.meals.models.DoubleSerializer
import de.nogaemer.springhomepage.main.meals.models.MealIngredient
import de.nogaemer.springhomepage.main.notes.Note
import de.nogaemer.springhomepage.main.ratings.Rating
import de.nogaemer.springhomepage.main.tags.Tag
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

    val ingredients: List<MealIngredient>,

    val instructions: List<String>,

    val images: List<Image>,

    val imageSrc: List<String>,

    val imageSrcSet: List<String>,

    @NotNull
    val difficulty: String,

    val time: Long,

    val portions: Int,

    val calories: Int,

    val url: String = "",

    @NotNull
    var tags: List<Tag> = emptyList(),

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

