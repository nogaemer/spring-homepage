package de.nogaemer.springhomepage.meals.ratings

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document(collection = "ratings")
@Data
@NoArgsConstructor @AllArgsConstructor
data class Rating(
    @field:JsonSerialize(using = ToStringSerializer::class)
    val mealId: ObjectId,
    val rating: Int
){
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    private var id: ObjectId? = null

    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: ObjectId? = null
}