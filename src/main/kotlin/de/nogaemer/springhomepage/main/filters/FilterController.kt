package de.nogaemer.springhomepage.main.filters

import de.nogaemer.springhomepage.main.meals.dto.MealCardDto
import de.nogaemer.springhomepage.user.UserResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for meal filtering and search operations.
 *
 * Provides various endpoints for discovering and filtering meals based on
 * different criteria including ingredients, ratings, names, and user preferences.
 * All endpoints are mapped under /api/v1/filters base path.
 *
 * ## Endpoint Categories
 * - **Filter Metadata**: Get available filter options and users
 * - **Favorites**: Retrieve user's highly-rated meals
 * - **Ingredient-Based**: Find meals cookable with available ingredients
 * - **Name Search**: Text-based meal discovery
 *
 * ## Response Format
 * All endpoints return ResponseEntity with HTTP 200 OK status.
 * Response bodies contain domain-specific DTOs or lists.
 *
 * ## Authentication
 * Some endpoints (like /favorite) require authentication to access
 * current user context. Check Spring Security configuration for details.
 *
 * @property filterService Service layer handling all filter business logic
 *
 * @see FilterService
 * @see FilterResponse
 * @see MealCardDto
 */
@RestController
@RequestMapping("/api/v1/filters")
class FilterController(
    val filterService: FilterService
) {

    /**
     * GET /api/v1/filters
     *
     * Retrieves all available filter options for the UI.
     *
     * Returns comprehensive metadata needed to populate filter dropdowns
     * and controls in the frontend, including user lists and sort parameters.
     *
     * ## Response Contents
     * - List of all registered users for "created by" filtering
     * - Available sort options with display names and default selection
     *
     * ## Usage
     * Called once when loading the meal browsing interface to populate
     * filter controls with available options.
     *
     * @return ResponseEntity containing [FilterResponse] with all filter metadata
     */
    @GetMapping
    fun getFilters(): ResponseEntity<FilterResponse> {
        return ResponseEntity.ok().body(filterService.getFilters())
    }

    /**
     * GET /api/v1/filters/users
     *
     * Searches for users by name with optional filtering.
     *
     * Supports autocomplete functionality for user selection in filters
     * or meal creator assignment.
     *
     * ## Query Parameters
     * - **name** (optional): Filter users whose names contain this string (case-insensitive)
     *
     * ## Behavior
     * - If name is null/empty: Returns all users
     * - If name provided: Returns users matching the name pattern
     *
     * ## Use Cases
     * - Autocomplete for "created by" filter
     * - User selection in meal editing
     * - Admin user browsing
     *
     * @param name Optional search term for filtering user names
     * @return ResponseEntity containing list of [UserResponse] matching criteria
     */
    @GetMapping("/users")
    fun getUsers(
        @RequestParam name: String?
    ): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok().body(filterService.getUsers(name))
    }

    /**
     * GET /api/v1/filters/favorite
     *
     * Retrieves the current user's favorite meals based on their ratings.
     *
     * Fetches meals that the authenticated user has rated at or above the
     * specified minimum rating threshold.
     *
     * ## Query Parameters
     * - **minRating** (optional, default: 4): Minimum rating threshold (1-5)
     *
     * ## Authentication Required
     * Requires valid JWT token to identify current user.
     *
     * ## Use Cases
     * - "My Favorites" page
     * - Personalized meal recommendations
     * - Quick access to user's top-rated meals
     *
     * ## Response
     * List of meal cards with full details including images and ingredients,
     * filtered by user's ratings.
     *
     * @param minRating Minimum rating threshold (defaults to 4 stars)
     * @return ResponseEntity containing list of [MealCardDto] for favorite meals
     */
    @GetMapping("/favorite")
    fun getMyFavoriteMeals(
        @RequestParam(defaultValue = "4") minRating: Int
    ): ResponseEntity<List<MealCardDto>> {
        return ResponseEntity.ok().body(filterService.getMyFavoriteMeals(minRating = minRating))
    }

    /**
     * GET /api/v1/filters/by-ingredients
     *
     * Finds meals that can be prepared with specified ingredients.
     *
     * Calculates ingredient match ratios and returns meals that meet the
     * minimum match threshold, ordered by best matches first.
     *
     * ## Query Parameters
     * - **ingredients** (required): List of ingredient ObjectId strings
     * - **minMatch** (optional, default: 0.5): Minimum match ratio (0.0-1.0)
     *
     * ## Match Ratio
     * Calculated as: (matching ingredients) / (total meal ingredients)
     * - 1.0 = 100% match (all ingredients available)
     * - 0.5 = 50% match (half of ingredients available)
     *
     * ## Response
     * List of meals sorted by match ratio (descending). Each meal includes
     * a matchingRatio field indicating the percentage of ingredients available.
     *
     * ## Use Case
     * "What can I cook?" feature - helps users find meals they can prepare
     * with ingredients they already have.
     *
     * @param ingredients List of available ingredient IDs
     * @param minMatch Minimum match ratio (default: 0.5 = 50%)
     * @return ResponseEntity containing list of [MealCardDto] with matching meals
     */
    @GetMapping("/by-ingredients")
    fun getMealsByIngredients(
        @RequestParam ingredients: List<String>,
        @RequestParam(defaultValue = "0.5") minMatch: Double
    ): ResponseEntity<List<MealCardDto>> {
        return ResponseEntity.ok().body(filterService.getByIngredients(ingredients, minMatch))
    }


    /**
     * GET /api/v1/filters/by-name
     *
     * Searches meals by name using intelligent multi-token matching.
     *
     * Implements fuzzy search with relevance scoring based on how many
     * search terms match the meal name.
     *
     * ## Query Parameters
     * - **name** (required): Search query (can contain multiple words)
     *
     * ## Search Behavior
     * - Tokenizes query into individual words
     * - Matches meals containing any of the tokens (case-insensitive)
     * - Scores by number of matching tokens
     * - Orders results by relevance (highest score first)
     *
     * ## Examples
     * - "chocolate cake" matches "Chocolate Lava Cake" (score: 2)
     * - "chocolate cake" matches "Vanilla Cake" (score: 1)
     *
     * ## Use Cases
     * - Main search bar functionality
     * - Meal name autocomplete
     * - Quick meal discovery
     *
     * @param name Search query string
     * @return ResponseEntity containing list of [MealCardDto] ordered by relevance
     */
    @GetMapping("/by-name")
    fun getMealsByName(
        @RequestParam name: String
    ) : ResponseEntity<List<MealCardDto>>{
        return ResponseEntity.ok().body(filterService.searchByName(name))
    }




}