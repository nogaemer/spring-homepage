package de.nogaemer.springhomepage.meals.notes

import EntityWithMealId
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
data class Note(
    @field:JsonSerialize(using = ToStringSerializer::class)
    override val mealId: ObjectId,
    val note: String
): EntityWithMealId{
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    private var id: ObjectId? = null

    @field:JsonSerialize(using = ToStringSerializer::class)
    override var userId: ObjectId? = null
}