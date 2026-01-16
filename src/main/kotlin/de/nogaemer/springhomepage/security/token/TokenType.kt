package de.nogaemer.springhomepage.security.token

/**
 * Enumeration of supported token types for authentication.
 *
 * Currently only BEARER tokens are supported, following OAuth 2.0 Bearer Token specification.
 * Bearer tokens are included in the Authorization header: `Authorization: Bearer <token>`
 *
 * Future token types could include: BASIC, DIGEST, etc.
 */
enum class TokenType {
    /**
     * Bearer token type per RFC 6750.
     * Used for JWT tokens in Authorization header.
     */
    BEARER
}
