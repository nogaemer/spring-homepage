package de.nogaemer.springhomepage.logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async

/**
 * Service for asynchronous logging operations.
 *
 * Provides non-blocking logging functionality using Spring's @Async support,
 * allowing log operations to execute in separate threads without blocking
 * the main application flow.
 *
 * ## Purpose
 * Prevents logging from impacting request response times by:
 * - Executing log operations in background threads
 * - Avoiding I/O blocking on main request threads
 * - Improving overall application throughput
 *
 * ## Async Configuration
 * Requires [de.nogaemer.springhomepage.config.AsyncConfig] to be enabled.
 * Methods marked with @Async execute in Spring's task executor thread pool.
 *
 * ## Use Cases
 * - High-frequency logging scenarios
 * - Logging from time-sensitive operations
 * - Audit trail logging
 * - Debug logging in production
 *
 * ## Method Visibility
 * Methods must be:
 * - Declared as `open` (not final) for Spring proxy generation
 * - Called from outside the class (self-invocation doesn't trigger async)
 * - Public visibility for proxy interception
 *
 * ## Logging Backend
 * Uses SLF4J (Simple Logging Facade for Java) which:
 * - Provides abstraction over logging implementations
 * - Typically backed by Logback in Spring Boot
 * - Supports configurable log levels and appenders
 *
 * ## Example Usage
 * ```kotlin
 * @Service
 * class MyService(private val logService: LogService) {
 *     fun processRequest() {
 *         logService.logAsync("Processing started")
 *         // ... business logic ...
 *         logService.logAsync("Processing completed")
 *     }
 * }
 * ```
 *
 * ## Thread Pool Configuration
 * Uses Spring's default async executor unless custom executor configured.
 * See [AsyncConfig] for customization options.
 *
 * @see de.nogaemer.springhomepage.config.AsyncConfig
 * @see org.springframework.scheduling.annotation.Async
 */
open class LogService {
    /**
     * SLF4J logger instance for this service.
     *
     * Configured to log under the LogService class name, allowing
     * separate log level configuration if needed.
     */
    private val logger = LoggerFactory.getLogger(LogService::class.java)

    /**
     * Logs a message asynchronously at INFO level.
     *
     * Executes logging in a separate thread from Spring's async task executor,
     * returning immediately without blocking the caller.
     *
     * ## Async Behavior
     * - Returns immediately (non-blocking)
     * - Actual logging happens in background thread
     * - No guarantee on execution order relative to other async calls
     * - Exceptions in logging thread don't propagate to caller
     *
     * ## Log Level
     * Uses INFO level which typically means:
     * - Important application events
     * - Business logic milestones
     * - System state changes
     *
     * ## Important Notes
     * - Must be called from outside LogService class (proxy limitation)
     * - Method must be open (not final) for Spring proxying
     * - If called from within LogService, executes synchronously
     *
     * ## Thread Safety
     * SLF4J logger is thread-safe, allowing concurrent calls from multiple
     * threads without synchronization.
     *
     * @param message The message to log at INFO level
     */
    @Async
    open fun logAsync(message: String) {
        logger.info(message)
    }
}