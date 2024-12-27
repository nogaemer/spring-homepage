package de.nogaemer.springhomepage.main.notes

import de.nogaemer.springhomepage.main.meals.EntityWithMealId
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
data class Note(
    @field:JsonSerialize(using = ToStringSerializer::class)
    override var mealId: ObjectId,
    val note: String,

    @CreatedDate
    var date: LocalDateTime? = null,
    @LastModifiedDate
    var modifiedDate: LocalDateTime? = null
): de.nogaemer.springhomepage.main.meals.EntityWithMealId {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    private var id: ObjectId? = null

    @field:JsonSerialize(using = ToStringSerializer::class)
    override var userId: ObjectId? = null
}