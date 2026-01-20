package de.nogaemer.springhomepage.main.meals

import de.nogaemer.springhomepage.main.meals.cookhistory.MealCookHistoryService
import de.nogaemer.springhomepage.main.meals.dto.MealCardDto
import de.nogaemer.springhomepage.main.meals.dto.MealDto
import de.nogaemer.springhomepage.main.meals.dto.MealWithCookHistoryDto
import de.nogaemer.springhomepage.main.meals.dto.UnifiedMealSearchRequest
import de.nogaemer.springhomepage.main.meals.dto.UnifiedMealSearchResponse
import de.nogaemer.springhomepage.main.meals.dto.toMealWithCookHistory
import de.nogaemer.springhomepage.main.meals.import.MealImportUrl
import de.nogaemer.springhomepage.main.meals.models.Meal
import de.nogaemer.springhomepage.main.meals.models.MealImportMethod
import de.nogaemer.springhomepage.user.UserService
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * REST controller for meal-related endpoints.
 *
 * Provides comprehensive CRUD operations and search functionality for meal management.
 * All endpoints are mapped under /api/v1/meals base path.
 *
 * ## API Endpoints Summary
 * - POST /search - Advanced meal search with ingredient matching
 * - GET /all - Retrieve all meals as cards
 * - GET /{id} - Get single meal with full details
 * - GET /byFilter - Multi-dimensional meal filtering
 * - POST / - Create new meal
 * - PUT /{id} - Update existing meal
 * - DELETE /{id} - Delete meal and associated resources
 *
 * ## Response Format
 * All endpoints return ResponseEntity with appropriate HTTP status codes.
 * Success responses include entity/list in body. Errors throw exceptions
 * handled by global exception handlers.
 *
 * ## Authentication/Authorization
 * Not explicitly shown here but typically enforced via Spring Security
 * at method or class level (check for @PreAuthorize annotations).
 *
 * @property unifiedMealSearchService Service for advanced search operations
 * @property service Main meal service for CRUD operations
 *
 * @see MealService
 * @see Meal
 * @see MealDto
 * @see MealCardDto
 */
@RestController
@RequestMapping("/api/v1/meals")
class MealController(
    private val unifiedMealSearchService: UnifiedMealSearchService, 
    private val service: MealService,
    private val cookHistoryService: MealCookHistoryService,
    private val userService: UserService
) {

    /**
     * POST /api/v1/meals/search
     *
     * Advanced meal search with ingredient-based matching and relevance scoring.
     *
     * Supports searching by ingredient lists and calculates match scores to help
     * users find meals they can cook with available ingredients.
     *
     * ## Request Body
     * [UnifiedMealSearchRequest] containing search parameters (e.g., ingredient IDs,
     * search term, filters)
     *
     * ## Response
     * - **200 OK**: Returns [UnifiedMealSearchResponse] with matched meals and relevance scores
     *
     * ## Search Features
     * - Ingredient-based matching
     * - Relevance scoring
     * - Match ratio calculation
     * - Supports partial ingredient matches
     *
     * @param request Search criteria including ingredients and filters
     * @return ResponseEntity containing search results with matched meals
     */
    @PostMapping("/search")
    fun searchMeals(
        @RequestBody request: UnifiedMealSearchRequest
    ): ResponseEntity<UnifiedMealSearchResponse> {
        return ResponseEntity.ok(unifiedMealSearchService.search(request))
    }

    /**
     * GET /api/v1/meals/all
     *
     * Retrieves all meals as lightweight card representations.
     *
     * Returns cached list of all meals optimized for grid/list displays.
     * Each meal contains only summary information (name, rating, time, difficulty, images).
     *
     * ## Response
     * - **200 OK**: List of [MealCardDto] objects
     *
     * ## Performance
     * - Results are cached in MealService.findAll()
     * - Efficient for frequent "browse all meals" operations
     * - Consider pagination for large meal databases
     *
     * @return ResponseEntity containing list of all meals as cards
     */
    @GetMapping("/all")
    fun getAllMeals(): ResponseEntity<List<MealCardDto>> {
        return ResponseEntity<List<MealCardDto>>(service.findAll(), HttpStatus.OK)
    }

    /**
     * GET /api/v1/meals/{id}
     *
     * Retrieves a single meal with complete details and relationships.
     *
     * Returns full meal entity including ingredients, instructions, tags, ratings,
     * and notes. All @DocumentReference relationships are resolved via aggregation.
     * For authenticated users, also includes lastCookedAt timestamp.
     *
     * ## Path Parameters
     * - **id**: MongoDB ObjectId of the meal (hex string format)
     *
     * ## Response
     * - **200 OK**: Complete [MealWithCookHistoryDto] with all relationships populated
     *   and lastCookedAt for authenticated users, or [Meal] for anonymous users
     * - **400 Bad Request**: If id is null or invalid format
     * - **404 Not Found**: If meal doesn't exist (via exception handler)
     *
     * ## Use Cases
     * - Meal detail page display
     * - Recipe viewing
     * - Editing preparation (load existing data)
     *
     * ## Enhanced Features
     * When user is authenticated, includes:
     * - lastCookedAt: Timestamp of when user last cooked this meal
     *
     * @param id ObjectId of the meal to retrieve
     * @return ResponseEntity containing meal details with cook history if authenticated
     * @throws IllegalArgumentException If id is null
     */
    @GetMapping("/{id}")
    fun getSingleMeal(
        @PathVariable id: ObjectId?
    ): ResponseEntity<Any> {
        id ?: throw IllegalArgumentException("Id is null")

        val meal = service.findById(id)
        
        // Try to get authenticated user
        return try {
            val userId = userService.getCurrentUser().id?.toString()
            if (userId != null) {
                // User is authenticated - include cook history
                val lastCookedAt = cookHistoryService.getLastCookDateForMeal(userId, id.toString())
                ResponseEntity.ok(meal.toMealWithCookHistory(lastCookedAt))
            } else {
                // User not authenticated - return plain meal
                ResponseEntity.ok(meal)
            }
        } catch (e: Exception) {
            // User not authenticated or error getting user - return plain meal
            ResponseEntity.ok(meal)
        }
    }

    /**
     * GET /api/v1/meals/byFilter
     *
     * Filters meals by multiple criteria with advanced user-based rating support.
     *
     * Supports multi-dimensional filtering including name search, tag filtering,
     * time constraints, and user-specific rating calculations.
     *
     * ## Query Parameters
     * - **name** (optional): Partial name search (case-insensitive)
     * - **tags** (optional): Comma-separated tag list for category filtering
     * - **time** (optional): Maximum cooking time in minutes
     * - **users** (optional): Comma-separated user IDs for rating filtering
     *
     * ## Response
     * - **200 OK**: List of [MealCardDto] matching filters, sorted by rating
     * - **400 Bad Request**: If user IDs are invalid (via exception handler)
     *
     * ## Sorting
     * - With users parameter: Sorted by average rating from specified users
     * - Without users parameter: Sorted by overall meal rating
     *
     * ## Use Cases
     * - Filtered meal browsing ("vegetarian meals under 30 minutes")
     * - User-specific recommendations ("meals my family rated highly")
     * - Category exploration ("all desserts")
     *
     * @param name Optional name search term
     * @param users Optional comma-separated user ObjectIds
     * @param tags Optional comma-separated tag names
     * @param time Optional maximum cooking time
     * @return ResponseEntity containing filtered meals as cards
     */
    @GetMapping("/byFilter")
    fun getFiltertMeals(
        @RequestParam name: String?, @RequestParam users: String?, @RequestParam tags: String?, @RequestParam time: Int?
    ): ResponseEntity<List<MealCardDto>> {
        val response = service.filterMeals(name, users, tags, time)
        return ResponseEntity.ok(response)
    }

    /**
     * PUT /api/v1/meals/{id}
     *
     * Updates an existing meal with new data.
     *
     * Replaces meal data while preserving system-managed fields (id, url, rating,
     * ratings list, notes list). All DTO fields are validated and processed.
     *
     * ## Path Parameters
     * - **id**: MongoDB ObjectId of the meal to update
     *
     * ## Request Body
     * [MealDto] containing updated meal data (name, ingredients, instructions, etc.)
     *
     * ## Response
     * - **200 OK**: Updated [Meal] entity
     * - **400 Bad Request**: If id is null or validation fails
     * - **404 Not Found**: If meal doesn't exist (via exception handler)
     *
     * ## Validation
     * - Meal must exist
     * - Ingredient units must be valid
     * - Tags must exist in system
     *
     * ## Cache Impact
     * Invalidates meal-specific and allMeals caches
     *
     * @param id ObjectId of the meal to update
     * @param meal MealDto with updated data
     * @return ResponseEntity containing updated meal entity
     * @throws IllegalArgumentException If id is null
     */
    @PutMapping("/{id}")
    fun updateMeal(
        @PathVariable id: ObjectId?, @RequestBody meal: MealDto
    ): ResponseEntity<Meal> {
        id ?: throw IllegalArgumentException("Id is null")

        return ResponseEntity<Meal>(service.update(id, meal), HttpStatus.OK)
    }

    /**
     * POST /api/v1/meals
     *
     * Creates a new meal from provided data.
     *
     * Validates meal data, resolves ingredient references, and saves to database.
     * Duplicate meal names are rejected.
     *
     * ## Request Body
     * [MealDto] containing complete meal data
     *
     * ## Response
     * - **201 CREATED**: Created [Meal] entity with generated ID
     * - **400 Bad Request**: If validation fails (via exception handler)
     * - **409 Conflict**: If meal name already exists (via AlreadyReported exception)
     *
     * ## Validation
     * - Meal name must be unique
     * - All ingredient units must exist
     * - Tags must exist in system
     *
     * ## Cache Impact
     * Invalidates allMeals cache
     *
     * @param meal MealDto containing new meal data
     * @return ResponseEntity containing created meal with generated ID
     */
    @PostMapping
    fun createMeal(
        @RequestBody meal: MealDto
    ): ResponseEntity<Meal> {
        return ResponseEntity<Meal>(service.create(meal), HttpStatus.CREATED)
    }

    /**
     * DELETE /api/v1/meals/{id}
     *
     * Deletes a meal and all associated resources.
     *
     * Removes meal from database and cleans up:
     * - Appwrite storage images
     * - User ratings
     * - Cache entries
     *
     * ## Path Parameters
     * - **id**: MongoDB ObjectId of the meal to delete
     *
     * ## Response
     * - **204 NO CONTENT**: Meal successfully deleted (empty response body)
     * - **400 Bad Request**: If id is null
     * - **404 Not Found**: If meal doesn't exist (via exception handler)
     *
     * ## Cleanup Operations
     * 1. Deletes images from Appwrite storage (failures logged, not blocking)
     * 2. Deletes all user ratings for this meal
     * 3. Removes meal document from MongoDB
     * 4. Invalidates meal-specific and allMeals caches
     *
     * ## Side Effects
     * - User ratings are permanently deleted
     * - Image files are removed from cloud storage
     * - Notes may be orphaned (check cascade behavior)
     *
     * @param id ObjectId of the meal to delete
     * @return ResponseEntity with 204 NO CONTENT status
     * @throws IllegalArgumentException If id is null
     */
    @DeleteMapping("/{id}")
    fun deleteMeal(
        @PathVariable id: ObjectId?
    ): ResponseEntity<Void> {
        id ?: throw IllegalArgumentException("Id is null")

        service.deleteById(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}

/**
 * REST controller for importing meals from external recipe sources.
 *
 * Provides endpoints for importing and previewing recipes from supported
 * external websites. All endpoints are mapped under /api/v1/import base path.
 *
 * ## Supported Sources
 * - Chefkoch.de (German recipe website)
 *
 * ## Import Process
 * 1. Validates URL matches expected domain
 * 2. Scrapes recipe data from source
 * 3. Converts to Meal entity
 * 4. Optionally saves to database
 *
 * @property service MealService for import operations
 */
@RestController
@RequestMapping("/api/v1/import")
class MealImportController {
    @Autowired
    private val service: MealService? = null

    /**
     * GET /api/v1/import/meal
     *
     * Previews a meal import without saving to database.
     *
     * Scrapes and parses recipe from external URL, returning the Meal entity
     * without persisting it. Useful for preview/validation before importing.
     *
     * ## Query Parameters
     * - **importTag** (optional): Import source identifier (default: "chefkoch")
     * - **url** (required): Complete URL of the recipe page
     *
     * ## Response
     * - **201 CREATED**: Parsed [Meal] entity (not persisted, status code is misleading)
     * - **400 Bad Request**: If URL is null or doesn't match expected domain
     *
     * ## Note
     * HTTP 201 status is semantically incorrect as no resource is created.
     * Consider using 200 OK for preview operations.
     *
     * @param importTag Source identifier (e.g., "chefkoch")
     * @param url Recipe page URL
     * @return ResponseEntity containing parsed meal (not saved)
     * @throws IllegalArgumentException If url is null or invalid
     */
    @GetMapping("/meal")
    fun getMeal(
        @RequestParam(value = "importTag", defaultValue = "chefkoch") importTag: String,
        @RequestParam(value = "url") url: String?,
    ): ResponseEntity<Meal> {
        url ?: throw IllegalArgumentException("Url is null")

        val tag = MealImportMethod.valueOf(importTag.uppercase())

        return ResponseEntity(service!!.importMealAsync(tag, url, false).get(), HttpStatus.CREATED)
    }

    /**
     * POST /api/v1/import/meal
     *
     * Imports and saves a meal from external recipe source.
     *
     * Scrapes recipe from provided URL, parses into Meal entity, and saves to
     * database. Includes duplicate URL detection.
     *
     * ## Query Parameters
     * - **importTag** (optional): Import source identifier (default: "chefkoch")
     *
     * ## Request Body
     * [MealImportUrl] containing the recipe URL
     *
     * ## Response
     * - **201 CREATED**: Created [Meal] entity with generated ID
     * - **400 Bad Request**: If URL doesn't match expected domain
     * - **409 Conflict**: If recipe URL already imported (via AlreadyReported exception)
     *
     * ## Validation
     * - URL must match expected domain for import source
     * - URL must not already exist in database
     *
     * ## Async Processing
     * Import runs asynchronously but blocks until complete via CompletableFuture.get().
     * Consider returning 202 Accepted with job ID for true async processing.
     *
     * @param importTag Source identifier
     * @param import Object containing recipe URL
     * @return ResponseEntity containing imported and saved meal
     */
    @PostMapping("/meal")
    fun createMeal(
        @RequestParam(value = "importTag", defaultValue = "chefkoch") importTag: String,
        @RequestBody import: MealImportUrl
    ): ResponseEntity<Meal> {
        val tag = MealImportMethod.valueOf(importTag.uppercase())

        return ResponseEntity(service!!.importMealAsync(tag, import.url).get(), HttpStatus.CREATED)
    }
}