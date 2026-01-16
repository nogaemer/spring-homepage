package de.nogaemer.springhomepage.user

import lombok.RequiredArgsConstructor

/**
 * Enumeration of fine-grained permissions for role-based access control.
 *
 * Defines specific operations that can be granted to roles. Each permission
 * represents a distinct capability that can be checked in authorization logic.
 *
 * ## Permission Categories:
 *
 * ### Admin Permissions:
 * Administrative operations including user management, system configuration,
 * and full access to all resources. Only granted to ADMIN role.
 *
 * ### Manager Permissions:
 * Management operations for handling business resources, content management,
 * and operational tasks. Granted to MANAGER and ADMIN roles.
 *
 * ## Usage with Spring Security:
 * Permissions are used in @PreAuthorize annotations to secure endpoints:
 * ```
 * @PreAuthorize("hasAuthority('admin:create')")
 * fun adminOperation() { ... }
 * ```
 *
 * ## Permission String Format:
 * Follows pattern `{scope}:{operation}` where:
 * - scope: "admin" or "management"
 * - operation: "read", "update", "create", or "delete"
 *
 * @property permission String value used by Spring Security for authorization checks
 */
@RequiredArgsConstructor
enum class Permission(val permission: String) {
    /**
     * Read access to administrative resources.
     * Allows viewing admin panels, user lists, system settings, etc.
     */
    ADMIN_READ("admin:read"),
    
    /**
     * Update access to administrative resources.
     * Allows modifying system settings, updating user information, etc.
     */
    ADMIN_UPDATE("admin:update"),
    
    /**
     * Create access to administrative resources.
     * Allows creating new users, system configurations, etc.
     */
    ADMIN_CREATE("admin:create"),
    
    /**
     * Delete access to administrative resources.
     * Allows removing users, deleting system data, etc.
     */
    ADMIN_DELETE("admin:delete"),
    
    /**
     * Read access to management resources.
     * Allows viewing business data, reports, operational information, etc.
     */
    MANAGER_READ("management:read"),
    
    /**
     * Update access to management resources.
     * Allows modifying business data, content, operational settings, etc.
     */
    MANAGER_UPDATE("management:update"),
    
    /**
     * Create access to management resources.
     * Allows creating new business entities, content, etc.
     */
    MANAGER_CREATE("management:create"),
    
    /**
     * Delete access to management resources.
     * Allows removing business entities, content, etc.
     */
    MANAGER_DELETE("management:delete")
}
