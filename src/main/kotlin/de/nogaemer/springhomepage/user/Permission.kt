package de.nogaemer.springhomepage.user

import lombok.Getter
import lombok.RequiredArgsConstructor

@RequiredArgsConstructor
enum class Permission(val permission: String) {
    ADMIN_READ("admin:read"),
    ADMIN_UPDATE("admin:update"),
    ADMIN_CREATE("admin:create"),
    ADMIN_DELETE("admin:delete"),
    MANAGER_READ("management:read"),
    MANAGER_UPDATE("management:update"),
    MANAGER_CREATE("management:create"),
    MANAGER_DELETE("management:delete")
}
