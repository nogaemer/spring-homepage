package de.nogaemer.springhomepage.user

import lombok.RequiredArgsConstructor
import org.springframework.security.core.authority.SimpleGrantedAuthority

@RequiredArgsConstructor
enum class Role(val permissions: Set<Permission>) {
    USER(emptySet()),
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
    MANAGER(
        setOf(
            Permission.MANAGER_READ,
            Permission.MANAGER_UPDATE,
            Permission.MANAGER_DELETE,
            Permission.MANAGER_CREATE
        )
    );

    fun getAuthorities(): List<SimpleGrantedAuthority> {
        val authorities = permissions.map { SimpleGrantedAuthority(it.permission) }.toMutableList()
        authorities.add(SimpleGrantedAuthority("ROLE_$name"))
        return authorities
    }

}
