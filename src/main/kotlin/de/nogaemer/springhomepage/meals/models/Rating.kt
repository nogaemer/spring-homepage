package de.nogaemer.springhomepage.meals.models

import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime


@Document(collection = "ratings")
@Data
@NoArgsConstructor @AllArgsConstructor
data class Rating(
    val mealId: ObjectId,
    val userId: ObjectId,
    val rating: Int
){
    @Id
    private var id: ObjectId? = null
}