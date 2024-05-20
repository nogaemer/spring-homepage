package de.nogaemer.springhomepage.user

import lombok.Builder
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@Builder
class ChangePasswordRequest {
    val currentPassword: String? = null
    val newPassword: String? = null
    val confirmationPassword: String? = null
}
