package de.nogaemer.springhomepage.main.meals.tags

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "tags")
data class Tag(

    @Id
    val id: String,

    val name: String,

    @field:JsonSerialize(using = ObjectIdListSerializer::class)
    var meals: MutableList<ObjectId> = mutableListOf()
)

class ObjectIdListSerializer : JsonSerializer<List<ObjectId>>() {
    override fun serialize(value: List<ObjectId>, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartArray()
        value.forEach { objectId ->
            gen.writeString(objectId.toString())
        }
        gen.writeEndArray()
    }
}