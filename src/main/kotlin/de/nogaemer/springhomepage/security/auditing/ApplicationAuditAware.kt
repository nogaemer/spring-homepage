package de.nogaemer.springhomepage.security.auditing

import de.nogaemer.springhomepage.user.User
import org.bson.types.ObjectId
import org.springframework.data.domain.AuditorAware
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

class ApplicationAuditAware : AuditorAware<ObjectId> {
    override fun getCurrentAuditor(): Optional<ObjectId> {
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
        return Optional.ofNullable(userPrincipal.id!!)
    }
}
