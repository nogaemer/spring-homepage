/**
 * Service orchestrating database migration and update scripts.
 *
 * This service acts as a central dispatcher for database schema migrations and data
 * transformations. Each migration script is assigned a unique numeric ID and can be
 * executed independently through this service.
 *
 * ## Migration Strategy
 * The application uses a manual migration approach where:
 * - Each migration is implemented as a separate Spring bean
 * - Migrations are triggered manually via REST API (not automatically on startup)
 * - Each migration is idempotent and can be re-run safely
 * - Migration status and results are returned for verification
 *
 * ## Available Migrations
 * - ID 1: LinkInMeal_11_16_2024 - Image data structure transformation
 * - ID 2: IngredientUnit_12_27_2025 - Unit string to ObjectId references
 * - ID 3: IngredientUnitForIngredients_01_02_2026 - Ingredient name to ObjectId references
 *
 * @property linkInMeal_11_16_2024 Migration for meal image link consolidation
 * @property ingredientUnit_12_27_2025 Migration for ingredient unit reference conversion
 * @property ingredientUnitForIngredients_01_02_2026 Migration for ingredient reference conversion
 * @see UpdateController
 */
package de.nogaemer.springhomepage.updatedb

import org.springframework.stereotype.Service

/**
 * Database update orchestration service.
 *
 * Coordinates the execution of database migration scripts by delegating to the
 * appropriate migration bean based on the provided update ID.
 */
@Service
class UpdateService(
    val linkInMeal_11_16_2024: LinkInMeal_11_16_2024,
    val ingredientUnit_12_27_2025: IngredientUnit_12_27_2025,
    val ingredientUnitForIngredients_01_02_2026: IngredientUnitForIngredients_01_02_2026
) {
    /**
     * Executes a specific database migration by its numeric identifier.
     *
     * Each migration ID maps to a specific update script that performs schema
     * transformations or data migrations. Migrations are designed to be idempotent
     * and safe to re-run.
     *
     * @param updateId Numeric identifier of the migration to execute:
     *   - 1: Image link consolidation (11/16/2024)
     *   - 2: Ingredient unit ObjectId conversion (12/27/2025)
     *   - 3: Ingredient reference ObjectId conversion (01/02/2026)
     * @return Migration result object containing update statistics, or empty Any() if ID is invalid
     * @throws Exception if the migration encounters errors during execution
     */
    fun update(updateId: Int): Any {
        when (updateId) {
            1 -> {
                return linkInMeal_11_16_2024.updateAll()
            }
            2 -> {
                return ingredientUnit_12_27_2025.updateAll()
            }
            3 -> {
                return ingredientUnitForIngredients_01_02_2026.updateAll()
            }
        }

        return Any()
    }
}