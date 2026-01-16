package de.nogaemer.springhomepage.user

import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

/**
 * REST controller for user management endpoints.
 *
 * Provides HTTP endpoints for:
 * - Changing user password
 * - Retrieving current user information
 *
 * All endpoints require authentication (JWT token in Authorization header).
 * All endpoints are under `/api/v1/users` base path.
 *
 * @property service User service for business logic
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
class UserController {

    @Autowired
    private val service: UserService? = null

    /**
     * Changes the password for the currently authenticated user.
     *
     * Endpoint: `PATCH /api/v1/users`
     *
     * **Security**: Requires authentication (any authenticated user can change their own password)
     *
     * Validates current password before allowing change.
     *
     * @param request Password change request containing current, new, and confirmation passwords
     * @param connectedUser Automatically injected Principal representing authenticated user
     * @return ResponseEntity with 200 OK on success
     * @throws IllegalStateException if current password is wrong or new passwords don't match
     */
    @PatchMapping
    fun changePassword(
        @RequestBody request: ChangePasswordRequest?,
        connectedUser: Principal?
    ): ResponseEntity<*> {
        service!!.changePassword(request!!, connectedUser!!)
        return ResponseEntity.ok().build<Any>()
    }

    /**
     * Retrieves information about the currently authenticated user.
     *
     * Endpoint: `GET /api/v1/users/me`
     *
     * **Security**: Requires authentication
     *
     * Returns basic user information (ID and name) without sensitive data.
     *
     * @param connectedUser Automatically injected Principal representing authenticated user
     * @return ResponseEntity containing UserResponse with user ID and name
     */
    @GetMapping("/me")
    fun getConnectedUser(
        connectedUser: Principal?
    ): ResponseEntity<*> {
        return ResponseEntity.ok(service!!.getConnectedUser(connectedUser!!))
    }
}
