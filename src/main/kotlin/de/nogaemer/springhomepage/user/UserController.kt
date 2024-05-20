package de.nogaemer.springhomepage.user

import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
class UserController {

    @Autowired
    private val service: UserService? = null

    @PatchMapping
    fun changePassword(
        @RequestBody request: ChangePasswordRequest?,
        connectedUser: Principal?
    ): ResponseEntity<*> {
        service!!.changePassword(request!!, connectedUser!!)
        return ResponseEntity.ok().build<Any>()
    }
}
