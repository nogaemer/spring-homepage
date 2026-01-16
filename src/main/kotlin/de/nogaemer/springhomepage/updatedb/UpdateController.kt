/**
 * REST API controller for manually triggering database migrations.
 *
 * This controller provides an HTTP interface for executing database update scripts
 * on-demand. Unlike automatic migration frameworks (like Flyway or Liquibase), these
 * migrations are triggered manually by administrators when needed.
 *
 * ## Usage
 * Execute a migration via:
 * ```
 * GET /api/v1/updateDB?updateId=1
 * ```
 *
 * ## Security Considerations
 * WARNING: This endpoint has no authentication or authorization checks. In a production
 * environment, this should be:
 * - Protected by admin-only authentication
 * - Restricted to internal networks only
 * - Disabled or removed after migrations are complete
 *
 * @property service The UpdateService that orchestrates migration execution
 * @see UpdateService
 */
package de.nogaemer.springhomepage.updatedb

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller exposing database migration operations.
 *
 * Provides HTTP endpoints for triggering database schema and data migrations.
 */
@RestController
@RequestMapping("/api/v1/updateDB")
class UpdateController(val service: UpdateService?) {

    /**
     * Executes a database migration by its numeric identifier.
     *
     * This endpoint triggers a specific migration script and returns the results.
     * Each migration is identified by a unique integer ID corresponding to a
     * specific update script.
     *
     * ## Available Migration IDs
     * - 1: LinkInMeal_11_16_2024 - Image structure transformation
     * - 2: IngredientUnit_12_27_2025 - Unit reference conversion
     * - 3: IngredientUnitForIngredients_01_02_2026 - Ingredient reference conversion
     *
     * ## Response Format
     * The response structure varies by migration but typically includes:
     * - Statistics (e.g., number of documents updated)
     * - Error information if the migration failed
     * - Status indicators
     *
     * @param updateID Numeric identifier of the migration to execute (1, 2, or 3)
     * @return ResponseEntity containing migration results and HTTP 200 OK status
     * @throws Exception if migration execution fails
     */
    @GetMapping
    fun getMealsByName(
        @RequestParam(
            value = "updateId",
        ) updateID: Int
    ): ResponseEntity<Any> {
        return ResponseEntity<Any>(service?.update(updateID), HttpStatus.OK)
    }
}