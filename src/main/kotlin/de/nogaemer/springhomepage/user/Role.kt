package de.nogaemer.springhomepage.user

import lombok.RequiredArgsConstructor
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * Enumeration of user roles with associated permissions.
 *
 * Defines a hierarchical role-based access control (RBAC) system where each role
 * has a specific set of permissions. Roles determine what operations a user can perform.
 *
 * ## Role Hierarchy:
 * - **USER**: Basic role with no special permissions. Can access public endpoints only.
 * - **MANAGER**: Has management permissions for read, update, create, and delete operations.
 * - **ADMIN**: Has all MANAGER permissions plus administrative permissions.
 *
 * ## Permissions System:
 * Each role contains a set of Permission enums that define fine-grained access control.
 * Spring Security uses these permissions with @PreAuthorize annotations to secure endpoints.
 *
 * Example: `@PreAuthorize("hasAuthority('admin:read')")` requires ADMIN role.
 *
 * ## Authority Generation:
 * The `getAuthorities()` method converts permissions to Spring Security authorities
 * and adds the role itself (prefixed with "ROLE_"). This allows both:
 * - Permission-based checks: `hasAuthority('admin:read')`
 * - Role-based checks: `hasRole('ADMIN')`
 *
 * @property permissions Set of permissions granted to this role
 */
@RequiredArgsConstructor
enum class Role(val permissions: Set<Permission>) {
    /**
     * Basic user role with no special permissions.
     * Can only access public endpoints and their own user data.
     */
    USER(emptySet()),
    
    /**
     * Administrative role with full system access.
     * Has all manager permissions plus admin-specific operations.
     * Can create users, manage system settings, and perform all operations.
     */
    ADMIN(
        setOf(
            Permission.ADMIN_READ,
            Permission.ADMIN_UPDATE,
            Permission.ADMIN_DELETE,
            Permission.ADMIN_CREATE,
            Permission.MANAGER_READ,
            Permission.MANAGER_UPDATE,
            Permission.MANAGER_DELETE,
            Permission.MANAGER_CREATE
        )
    ),
    
    /**
     * Management role with elevated permissions.
     * Can perform management operations like managing content,
     * viewing reports, and managing resources.
     */
    MANAGER(
        setOf(
            Permission.MANAGER_READ,
            Permission.MANAGER_UPDATE,
            Permission.MANAGER_DELETE,
            Permission.MANAGER_CREATE
        )
    );

    /**
     * Converts role permissions to Spring Security authorities.
     *
     * Creates a list of SimpleGrantedAuthority containing:
     * 1. All permissions from the role (e.g., "admin:read", "admin:update")
     * 2. The role itself with "ROLE_" prefix (e.g., "ROLE_ADMIN")
     *
     * Used by User.getAuthorities() to provide Spring Security with authorities
     * for authorization decisions.
     *
     * @return List of GrantedAuthority objects for this role
     */
    fun getAuthorities(): List<SimpleGrantedAuthority> {
        val authorities = permissions.map { SimpleGrantedAuthority(it.permission) }.toMutableList()
        authorities.add(SimpleGrantedAuthority("ROLE_$name"))
        return authorities
    }

}
