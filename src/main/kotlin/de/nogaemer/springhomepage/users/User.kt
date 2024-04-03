package de.nogaemer.springhomepage.users

import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "meals")
@Data
@AllArgsConstructor
@NoArgsConstructor
data class User(
    var id: ObjectId,
    var name: String,
    var password: String,
)