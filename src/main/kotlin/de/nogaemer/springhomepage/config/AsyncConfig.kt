package de.nogaemer.springhomepage.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

/**
 * Configuration class for enabling asynchronous method execution.
 *
 * Activates Spring's asynchronous processing capabilities, allowing methods
 * annotated with @Async to execute in separate threads from the thread pool.
 *
 * ## Functionality
 * - Enables `@Async` annotation support across the application
 * - Uses Spring's default thread pool executor unless custom executor is defined
 * - Allows non-blocking execution of time-consuming operations
 *
 * ## Default Behavior
 * - **Thread Pool**: SimpleAsyncTaskExecutor (creates new thread per task)
 * - **Exception Handling**: Uncaught exceptions are logged but don't propagate
 * - **Return Types**: Supports void, Future, CompletableFuture, ListenableFuture
 *
 * ## Use Cases
 * - Background logging (see [de.nogaemer.springhomepage.logger.LogService])
 * - Email sending
 * - External API calls
 * - Long-running computations
 *
 * ## Custom Configuration
 * To customize thread pool, define a bean named "taskExecutor":
 * ```kotlin
 * @Bean
 * fun taskExecutor(): Executor {
 *     return ThreadPoolTaskExecutor().apply {
 *         corePoolSize = 5
 *         maxPoolSize = 10
 *         queueCapacity = 100
 *         initialize()
 *     }
 * }
 * ```
 *
 * ## Important Notes
 * - Async methods must be called from outside the class (Spring proxy limitation)
 * - Methods must be public to be proxied
 * - Self-invocation won't trigger async behavior
 *
 * @see org.springframework.scheduling.annotation.Async
 * @see org.springframework.scheduling.annotation.EnableAsync
 * @see de.nogaemer.springhomepage.logger.LogService.logAsync
 */
@Configuration
@EnableAsync
class AsyncConfig