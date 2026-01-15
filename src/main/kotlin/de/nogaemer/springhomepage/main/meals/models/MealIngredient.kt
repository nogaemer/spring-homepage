package de.nogaemer.springhomepage.main.meals.models

import de.nogaemer.springhomepage.main.ingredients.Ingredient
import de.nogaemer.springhomepage.main.units.IngredientUnit
import org.springframework.data.mongodb.core.mapping.DocumentReference

/**
 * Represents an ingredient within a meal recipe, including quantity and unit information.
 *
 * This class is embedded within the [Meal] document and establishes relationships to
 * the centralized Ingredient and IngredientUnit collections via @DocumentReference.
 *
 * ## Storage Strategy
 * MealIngredient objects are embedded directly in the Meal document rather than stored
 * as separate MongoDB documents. This design choice optimizes read performance for recipe
 * display at the cost of slight duplication.
 *
 * ## MongoDB Annotations
 * - [@DocumentReference]: Creates lazy-loaded references to Ingredient and IngredientUnit
 *   documents stored in separate collections
 * - References are resolved via MongoDB aggregation lookups in complex queries
 *   (see MealService.findById aggregation pipeline)
 *
 * ## Relationships
 * - **Ingredient**: Reference to shared Ingredient entity (e.g., "tomato", "flour")
 * - **IngredientUnit**: Reference to measurement unit (e.g., "grams", "cups", "pieces")
 *
 * ## Usage Example
 * ```
 * MealIngredient(
 *   name = "Fresh tomatoes",
 *   amount = "500",
 *   ingredient = Ingredient(name = "Tomato", category = "Vegetables"),
 *   unit = IngredientUnit(abbreviation = "g", fullName = "grams")
 * )
 * ```
 *
 * @property name Display name for this ingredient in the recipe context (e.g., "Fresh tomatoes")
 * @property amount Quantity as a string to support fractions (e.g., "1.5", "1/2", "2-3")
 * @property ingredient Reference to the base ingredient entity from the ingredients collection
 * @property unit Reference to the measurement unit from the units collection
 *
 * @see Meal
 * @see Ingredient
 * @see IngredientUnit
 */
data class MealIngredient(
    val name: String = "",
    val amount: String = "",

    @DocumentReference
    val ingredient: Ingredient? = null,

    @DocumentReference
    val unit: IngredientUnit?
)