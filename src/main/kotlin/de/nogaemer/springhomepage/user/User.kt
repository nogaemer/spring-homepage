package de.nogaemer.springhomepage.user

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import de.nogaemer.springhomepage.security.token.Token
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.core.userdetails.UserDetails

/**
 * MongoDB entity representing a user with Spring Security UserDetails implementation.
 *
 * This class serves dual purposes:
 * 1. **MongoDB Entity**: Persisted in the "users" collection with @Document annotation
 * 2. **Spring Security Principal**: Implements UserDetails for authentication/authorization
 *
 * ## UserDetails Implementation:
 * Spring Security uses UserDetails to:
 * - Load user information during authentication
 * - Check account status (enabled, locked, expired)
 * - Retrieve authorities (roles and permissions)
 * - Validate credentials
 *
 * All account status methods return true (accounts are always enabled and not locked/expired).
 * Override these methods if implementing account lockout or expiration features.
 *
 * ## Role-Based Permissions:
 * Each user has a single Role (USER, MANAGER, or ADMIN) which determines their permissions.
 * Roles are hierarchical:
 * - USER: No special permissions
 * - MANAGER: management:read, management:update, management:create, management:delete
 * - ADMIN: All MANAGER permissions + admin:read, admin:update, admin:create, admin:delete
 *
 * The `getAuthorities()` method returns granted authorities combining role and permissions.
 *
 * ## MongoDB Annotations:
 * - @Document: Marks this as MongoDB collection "users"
 * - @Id: Marks ObjectId field as primary key (auto-generated)
 * - @DBRef: Creates lazy-loaded reference to Token documents
 * - @field:JsonSerialize: Converts ObjectId to string in JSON responses
 *
 * ## Tokens Relationship:
 * The @DBRef tokens field maintains a list of all JWT tokens issued to this user.
 * This allows:
 * - Tracking active sessions
 * - Revoking all user tokens
 * - Audit trail of authentication events
 *
 * @property name User's display name
 * @property login Unique login identifier (username or email) - used as Spring Security username
 * @property password Encoded password (BCrypt hash) - private to prevent exposure
 * @property role User's role determining permissions (USER, MANAGER, or ADMIN)
 * @property id MongoDB ObjectId (auto-generated, serialized as string in JSON)
 * @property tokens List of JWT tokens associated with this user (lazy-loaded via @DBRef)
 */
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

    /**
     * Returns authorities granted to the user based on their role.
     *
     * Combines role-based permissions (e.g., "admin:read") with role itself (e.g., "ROLE_ADMIN").
     * Used by Spring Security for authorization decisions (@PreAuthorize, hasAuthority, etc.).
     *
     * @return List of GrantedAuthority objects representing permissions and role
     */
    override fun getAuthorities() = role.getAuthorities()

    /**
     * Indicates whether the user's account is enabled.
     * Currently always returns true - all accounts are enabled.
     *
     * @return true (account is always enabled)
     */
    override fun isEnabled() = true

    /**
     * Returns the username used to authenticate the user.
     * Maps to the login field (username or email).
     *
     * @return User's login identifier
     */
    override fun getUsername() = login

    /**
     * Indicates whether the user's credentials (password) have expired.
     * Currently always returns true - credentials never expire.
     *
     * @return true (credentials are always valid)
     */
    override fun isCredentialsNonExpired() = true

    /**
     * Returns the password used to authenticate the user.
     * Returns the BCrypt encoded password hash.
     *
     * @return Encoded password hash
     */
    override fun getPassword() = password

    /**
     * Sets a new password for the user.
     * Should only be called with an already-encoded password.
     *
     * @param password Encoded password (BCrypt hash)
     */
    fun setPassword(password: String) {
        this.password = password
    }

    /**
     * Indicates whether the user's account has expired.
     * Currently always returns true - accounts never expire.
     *
     * @return true (account is always valid)
     */
    override fun isAccountNonExpired() = true

    /**
     * Indicates whether the user is locked or unlocked.
     * Currently always returns true - accounts are never locked.
     *
     * @return true (account is never locked)
     */
    override fun isAccountNonLocked() = true
}