package de.nogaemer.springhomepage.security.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.servers.Server

/**
 * OpenAPI (Swagger) configuration for API documentation.
 *
 * Configures Swagger UI with API metadata and JWT Bearer authentication scheme,
 * providing interactive API documentation accessible at `/swagger-ui.html`.
 *
 * ## API Documentation Info
 * - **Title**: OpenApi specification - Alibou
 * - **Version**: 1.0
 * - **Description**: OpenApi documentation for Spring Security
 * - Includes license and terms of service URLs
 *
 * ## Security Configuration
 * Defines JWT Bearer authentication scheme:
 * - **Scheme Name**: bearerAuth
 * - **Type**: HTTP Bearer
 * - **Format**: JWT (JSON Web Token)
 * - **Location**: Authorization header
 *
 * ## Usage
 * All API endpoints automatically inherit the `bearerAuth` security requirement,
 * meaning Swagger UI will prompt for JWT token before testing secured endpoints.
 *
 * ## Swagger UI Access
 * Once application is running:
 * - **Swagger UI**: http://localhost:8080/swagger-ui.html
 * - **OpenAPI JSON**: http://localhost:8080/v3/api-docs
 *
 * ## Authentication Flow
 * 1. Call `/api/v1/auth/authenticate` to get JWT token
 * 2. Click "Authorize" button in Swagger UI
 * 3. Enter token in format: `Bearer <token>`
 * 4. All subsequent requests include Authorization header
 *
 * ## Customization
 * Modify annotation attributes to update:
 * - API title, version, and description
 * - License and contact information
 * - Server URLs for different environments
 * - Security scheme configuration
 *
 * @see io.swagger.v3.oas.annotations.OpenAPIDefinition
 * @see io.swagger.v3.oas.annotations.security.SecurityScheme
 * @see de.nogaemer.springhomepage.security.config.JwtService
 */
@OpenAPIDefinition(
    info = Info(
        description = "OpenApi documentation for Spring Security",
        title = "OpenApi specification - Alibou",
        version = "1.0",
        license = License(name = "Licence name", url = "https://some-url.com"),
        termsOfService = "Terms of service"
    ),
    security = [SecurityRequirement(name = "bearerAuth")]
)
@SecurityScheme(
    name = "bearerAuth",
    description = "JWT auth description",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    `in` = SecuritySchemeIn.HEADER
)
class OpenApiConfig
