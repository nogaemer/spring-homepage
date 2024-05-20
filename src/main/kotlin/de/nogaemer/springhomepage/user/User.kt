package de.nogaemer.springhomepage.user

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.security.token.Token
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.core.userdetails.UserDetails

@Document(collection = "users")
data class User(

    val name: String,
    val login: String,
    private var password: String,
    var role: Role

) : UserDetails {
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: ObjectId? = null

    @DBRef
    var tokens: List<Token> = ArrayList()

    override fun getAuthorities() = role.getAuthorities()

    override fun isEnabled() = true

    override fun getUsername() = login

    override fun isCredentialsNonExpired() = true

    override fun getPassword() = password

    fun setPassword(password: String) {
        this.password = password
    }

    override fun isAccountNonExpired() = true

    override fun isAccountNonLocked() = true
}