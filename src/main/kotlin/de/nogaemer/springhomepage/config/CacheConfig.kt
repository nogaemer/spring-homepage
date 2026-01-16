package de.nogaemer.springhomepage.config

import org.springframework.context.annotation.Configuration
import org.springframework.cache.annotation.EnableCaching

/**
 * Configuration class for enabling Spring's caching infrastructure.
 *
 * Activates declarative cache management using annotations, allowing methods
 * to cache their results and avoid expensive recomputation or database queries.
 *
 * ## Functionality
 * - Enables `@Cacheable`, `@CachePut`, `@CacheEvict` annotations
 * - Uses Caffeine as the cache provider (configured via dependencies)
 * - Provides automatic cache synchronization across cache operations
 *
 * ## Cache Annotations
 * - **@Cacheable**: Return cached result if available, otherwise compute and cache
 * - **@CachePut**: Always execute method and update cache with result
 * - **@CacheEvict**: Remove entry/entries from cache
 * - **@Caching**: Group multiple cache operations
 *
 * ## Cache Names in Application
 * - **"meals"**: Individual meal lookups by ID
 * - **"allMeals"**: Full meal list queries
 * - Other domain-specific caches as needed
 *
 * ## Default Cache Provider
 * Uses Caffeine cache (high-performance Java caching library):
 * - In-memory, thread-safe caching
 * - Automatic size-based eviction
 * - Time-based expiration support
 * - Near-optimal hit rate
 *
 * ## Cache Invalidation Strategy
 * Caches are manually invalidated in services when data changes:
 * - Create/Update/Delete operations clear relevant caches
 * - See [de.nogaemer.springhomepage.main.meals.BaseService] for examples
 *
 * ## Performance Impact
 * - Dramatically reduces database load for frequently accessed data
 * - Typical meal queries cached to avoid MongoDB roundtrips
 * - Critical for browse/search operations with high read-to-write ratios
 *
 * ## Configuration Tuning
 * To customize cache behavior, define CacheManager bean:
 * ```kotlin
 * @Bean
 * fun cacheManager(): CacheManager {
 *     return CaffeineCacheManager().apply {
 *         setCaffeine(Caffeine.newBuilder()
 *             .maximumSize(1000)
 *             .expireAfterWrite(10, TimeUnit.MINUTES))
 *     }
 * }
 * ```
 *
 * @see org.springframework.cache.annotation.EnableCaching
 * @see org.springframework.cache.CacheManager
 * @see de.nogaemer.springhomepage.main.meals.BaseService
 * @see de.nogaemer.springhomepage.main.meals.MealService
 */
@Configuration
@EnableCaching
class CacheConfig