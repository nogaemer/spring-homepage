package de.nogaemer.springhomepage.meals.ratings

import de.nogaemer.springhomepage.meals.EntityWithMealId
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime


@Document(collection = "ratings")
@Data
@NoArgsConstructor @AllArgsConstructor
data class Rating(
    @field:JsonSerialize(using = ToStringSerializer::class)
    override var mealId: ObjectId,

    @NotNull
    var rating: Int,

    @CreatedDate
    var date: LocalDateTime? = null,

    @LastModifiedDate
    var modifiedDate: LocalDateTime? = null
): EntityWithMealId {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    private var id: ObjectId? = null

    @field:JsonSerialize(using = ToStringSerializer::class)
    override var userId: ObjectId? = null
}