package de.nogaemer.springhomepage.security.auditing

import de.nogaemer.springhomepage.user.User
import org.bson.types.ObjectId
import org.springframework.data.domain.AuditorAware
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

/**
 * Implementation of Spring Data's AuditorAware interface for entity auditing.
 *
 * Provides the current authenticated user's ID for automatic population of
 * auditing fields (createdBy, lastModifiedBy) in MongoDB entities that use
 * Spring Data's auditing annotations.
 *
 * ## Auditing Integration
 * Works with Spring Data MongoDB auditing annotations:
 * - `@CreatedBy`: Auto-populated on entity creation
 * - `@LastModifiedBy`: Auto-updated on entity modification
 * - `@CreatedDate`: Auto-populated on entity creation
 * - `@LastModifiedDate`: Auto-updated on entity modification
 *
 * ## Current Auditor Resolution
 * Extracts user ID from Spring Security context:
 * 1. Gets authentication from SecurityContextHolder
 * 2. Validates authentication is not null, authenticated, and not anonymous
 * 3. Casts principal to User and returns user's ObjectId
 * 4. Returns empty Optional if no valid authentication exists
 *
 * ## Use Cases
 * - Track which user created an entity
 * - Track which user last modified an entity
 * - Compliance and audit trail requirements
 * - Data ownership and accountability
 *
 * ## Configuration
 * Must be registered as a bean in Spring configuration:
 * ```kotlin
 * @Bean
 * fun auditorAware(): AuditorAware<ObjectId> = ApplicationAuditAware()
 * ```
 *
 * Also requires `@EnableMongoAuditing` on a configuration class.
 *
 * ## Security Context Dependency
 * Relies on [SecurityContextHolder] being properly populated by
 * authentication filters (typically JwtAuthenticationFilter) before
 * entity operations occur.
 *
 * ## Anonymous Users
 * Returns empty Optional for:
 * - Unauthenticated requests
 * - Anonymous authentication tokens
 * - Null authentication
 *
 * This prevents NPE and allows system operations without user context.
 *
 * @see org.springframework.data.domain.AuditorAware
 * @see org.springframework.data.annotation.CreatedBy
 * @see org.springframework.data.annotation.LastModifiedBy
 * @see de.nogaemer.springhomepage.security.config.JwtAuthenticationFilter
 */
class ApplicationAuditAware : AuditorAware<ObjectId> {
    /**
     * Returns the current auditor (authenticated user's ID).
     *
     * Extracts the user ID from Spring Security context to populate
     * audit fields in entities.
     *
     * ## Return Values
     * - **Optional.of(userId)**: When user is authenticated and valid
     * - **Optional.empty()**: When authentication is null, unauthenticated, or anonymous
     *
     * ## Process
     * 1. Retrieve authentication from SecurityContextHolder
     * 2. Validate authentication exists and is authenticated
     * 3. Check authentication is not anonymous
     * 4. Extract User principal
     * 5. Return user's ObjectId wrapped in Optional
     *
     * @return Optional containing current user's ObjectId, or empty if no valid user
     */
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
