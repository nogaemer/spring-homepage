package de.nogaemer.springhomepage.utils

import io.github.cdimascio.dotenv.Dotenv

/**
 * Utility object for accessing environment variables from multiple sources.
 *
 * Provides unified access to environment variables with automatic fallback
 * between .env files (development) and system environment (production).
 *
 * ## Variable Resolution Order
 * 1. First attempts to load from .env file (via dotenv library)
 * 2. Falls back to system environment variables if .env not available
 * 3. Returns null if variable not found in either source
 *
 * ## .env File Support
 * Uses dotenv-kotlin library to load variables from .env file in project root:
 * ```
 * DATABASE_URL=mongodb://localhost:27017
 * JWT_SECRET=my-secret-key
 * ```
 *
 * ## Use Cases
 * - Database connection strings
 * - API keys and secrets
 * - Service URLs and endpoints
 * - Feature flags
 * - Any configuration that differs between environments
 *
 * ## Development vs Production
 * - **Development**: Uses .env file for local configuration
 * - **Production**: Falls back to system environment (Docker, Kubernetes, etc.)
 * - **CI/CD**: Uses system environment (GitHub Actions, Jenkins, etc.)
 *
 * ## Error Handling
 * If .env file doesn't exist or can't be loaded:
 * - Silently catches exception and sets dotenv to null
 * - Continues working with system environment variables only
 * - No application crash or startup failure
 *
 * ## Security Considerations
 * - Never commit .env file to version control (.gitignore it)
 * - Use .env.example as template with dummy values
 * - In production, use proper secret management (Vault, AWS Secrets Manager, etc.)
 * - Avoid logging environment variable values
 *
 * ## Example Usage
 * ```kotlin
 * val dbUrl = EnvUtils.getEnvVariable("DATABASE_URL")
 * val jwtSecret = EnvUtils.getEnvVariable("JWT_SECRET") ?: "default-secret"
 * ```
 *
 * @see io.github.cdimascio.dotenv.Dotenv
 */
object EnvUtils {
    /**
     * Dotenv instance for loading .env file variables.
     *
     * Initialized once at class loading time:
     * - Attempts to load .env file from project root
     * - Sets to null if file doesn't exist or loading fails
     * - Null value triggers fallback to system environment
     */
    private val dotenv = try {
        Dotenv.load()
    } catch (e: Exception) {
        null
    }

    /**
     * Retrieves an environment variable from .env file or system environment.
     *
     * Checks both sources with automatic fallback:
     * 1. If dotenv loaded successfully, try getting from .env file
     * 2. If not in .env or dotenv is null, check system environment
     * 3. Return null if not found in either source
     *
     * ## Priority
     * .env file takes precedence over system environment, allowing
     * local overrides during development.
     *
     * ## Return Value
     * - String value if variable found
     * - null if variable not found in either source
     *
     * ## Thread Safety
     * This method is thread-safe as dotenv instance is immutable after
     * initialization and both dotenv.get() and System.getenv() are thread-safe.
     *
     * @param key Environment variable name (case-sensitive on Unix-like systems)
     * @return Variable value or null if not found
     */
    fun getEnvVariable(key: String): String? {
        return dotenv?.get(key) ?: System.getenv(key)
    }
}