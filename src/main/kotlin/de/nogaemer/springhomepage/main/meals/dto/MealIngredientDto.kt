package de.nogaemer.springhomepage.main.meals.dto

import de.nogaemer.springhomepage.main.ingredients.Ingredient
import de.nogaemer.springhomepage.main.units.IngredientUnitDto

/**
 * Data Transfer Object for ingredient information in meal creation/update requests.
 *
 * This DTO is used as part of [MealDto] to specify ingredients when creating or
 * updating meals. It references existing Ingredient entities and provides unit
 * information via [IngredientUnitDto].
 *
 * ## Resolution Process
 * When received in a request, this DTO is converted to a [MealIngredient] entity
 * by [MealService.resolveIngredients]:
 * 1. The [unit] DTO's ID is validated as a proper ObjectId
 * 2. The unit is fetched from the database via [UnitService.findById]
 * 3. A [MealIngredient] is created with the resolved Ingredient and IngredientUnit entities
 *
 * ## Validation
 * - [ingredient] must be a valid Ingredient entity
 * - [unit.id] must be a valid ObjectId
 * - The unit referenced by [unit.id] must exist in the database
 * - [amount] is stored as-is without validation (supports fractions and ranges)
 *
 * ## Error Handling
 * - Invalid unit ID format throws [IdNotFoundException]
 * - Non-existent unit throws [UnitNotFoundException]
 *
 * @property ingredient The base ingredient entity (must exist in ingredients collection)
 * @property amount Quantity as a string to support various formats (e.g., "2", "1.5", "1/2", "2-3")
 * @property unit Unit information including the ID reference for database lookup
 *
 * @see MealDto
 * @see de.nogaemer.springhomepage.main.meals.models.MealIngredient
 * @see MealService.resolveIngredients
 */
data class MealIngredientDto(
    val ingredient: Ingredient,
    val amount: String,
    val unit: IngredientUnitDto
)