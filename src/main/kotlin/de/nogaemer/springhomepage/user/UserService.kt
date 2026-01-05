package de.nogaemer.springhomepage.user

import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.Principal

@Service
@RequiredArgsConstructor
class UserService {
    @Autowired
    private val passwordEncoder: PasswordEncoder? = null

    @Autowired
    private val repository: UserRepository? = null


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

    fun getConnectedUser(connectedUser: Principal): Any {
        val user = (connectedUser as UsernamePasswordAuthenticationToken).principal as User
        return UserResponse(user.id!!, user.name)
    }

    fun getCurrentUser(): User {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.isAuthenticated) {
            return authentication.principal as User
        }
        throw RuntimeException("User not found")
    }
}