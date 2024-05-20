package de.nogaemer.springhomepage.security.auditing

import de.nogaemer.springhomepage.user.User
import org.springframework.data.domain.AuditorAware
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

class ApplicationAuditAware : AuditorAware<Int> {
    override fun getCurrentAuditor(): Optional<Int> {
        val authentication =
            SecurityContextHolder
                .getContext()
                .authentication
        if (authentication == null ||
            !authentication.isAuthenticated ||
            authentication is AnonymousAuthenticationToken
        ) {
            return Optional.empty()
        }

        val userPrincipal = authentication.principal as User
        return Optional.ofNullable(userPrincipal.id!!.timestamp)
    }
}
