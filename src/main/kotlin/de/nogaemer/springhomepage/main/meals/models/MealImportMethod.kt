package de.nogaemer.springhomepage.main.meals.models

/**
 * Defines the supported external sources for meal recipe imports.
 *
 * This enum is used by the meal import functionality to determine which parser
 * to use when scraping and importing recipes from external websites. Each value
 * corresponds to a specific implementation in the import package.
 *
 * ## Current Implementations
 * - **CHEFKOCH**: Parser for recipes from chefkoch.de, a German recipe website
 *
 * ## Usage
 * Used in [MealService.importMealAsync] to route import requests to the appropriate
 * parser implementation. The import process validates the URL matches the expected
 * domain for the selected import method.
 *
 * ## Adding New Import Sources
 * To add a new source:
 * 1. Add a new enum value here
 * 2. Create a parser implementation in the import package
 * 3. Add a case in MealService.importMealAsync
 *
 * @see de.nogaemer.springhomepage.main.meals.import.Chefkoch
 */
enum class MealImportMethod {
    CHEFKOCH;
}