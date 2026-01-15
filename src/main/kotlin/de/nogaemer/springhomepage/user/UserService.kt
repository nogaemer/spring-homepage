package de.nogaemer.springhomepage.user

import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.Principal

/**
 * Service for user management operations.
 *
 * Provides business logic for:
 * - Changing user passwords with validation
 * - Retrieving currently authenticated user information
 * - Accessing current user from security context
 *
 * @property passwordEncoder Encoder for hashing passwords (BCrypt)
 * @property repository Repository for user database operations
 */
@Service
@RequiredArgsConstructor
class UserService {
    @Autowired
    private val passwordEncoder: PasswordEncoder? = null

    @Autowired
    private val repository: UserRepository? = null


    /**
     * Changes a user's password with validation.
     *
     * ## Password Change Flow:
     * 1. Extracts User from Principal (authenticated user)
     * 2. Validates current password matches user's existing password
     * 3. Validates new password matches confirmation password
     * 4. Encodes new password with BCrypt
     * 5. Updates user entity with new encoded password
     * 6. Saves updated user to database
     *
     * @param request Password change request containing current, new, and confirmation passwords
     * @param connectedUser Principal representing the authenticated user making the request
     * @throws IllegalStateException if current password is incorrect
     * @throws IllegalStateException if new password and confirmation don't match
     */
    fun changePassword(request: ChangePasswordRequest, connectedUser: Principal) {
        val user = (connectedUser as UsernamePasswordAuthenticationToken).principal as User

        // check if the current password is correct
        check(passwordEncoder!!.matches(request.currentPassword, user.password)) { "Wrong password" }
        // check if the two new passwords are the same
        check(request.newPassword == request.confirmationPassword) { "Password are not the same" }

        // update the password
        user.password = passwordEncoder.encode(request.newPassword)

        // save the new password
        repository!!.save(user)
    }

    /**
     * Retrieves basic information about the currently authenticated user.
     *
     * Extracts user from Principal and returns a response DTO containing
     * user ID and name (no sensitive information like password).
     *
     * @param connectedUser Principal representing the authenticated user
     * @return UserResponse containing user ID and name
     */
    fun getConnectedUser(connectedUser: Principal): Any {
        val user = (connectedUser as UsernamePasswordAuthenticationToken).principal as User
        return UserResponse(user.id!!, user.name)
    }

    /**
     * Gets the currently authenticated user from Spring Security context.
     *
     * Retrieves the User object from SecurityContextHolder without requiring
     * a Principal parameter. Useful for service-level operations that need
     * current user context.
     *
     * @return Authenticated User entity
     * @throws RuntimeException if user is not authenticated or not found in context
     */
    fun getCurrentUser(): User {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.isAuthenticated) {
            return authentication.principal as User
        }
        throw RuntimeException("User not found")
    }
}