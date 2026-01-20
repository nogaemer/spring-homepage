package de.nogaemer.springhomepage.main.meals.cookhistory

import de.nogaemer.springhomepage.user.UserService
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * REST controller for cook history management.
 *
 * Provides endpoints for recording and retrieving user cooking history.
 * All endpoints require authentication to identify the current user.
 * All endpoints are mapped under /api/v1/history base path.
 *
 * ## Endpoint Summary
 * - POST /api/v1/history/{mealId}/log - Record a cooked meal
 * - GET /api/v1/history - Get paginated cook history
 * - GET /api/v1/history/meal/{mealId} - Get meal-specific cook history
 * - GET /api/v1/history/recent - Get recent cook history
 * - GET /api/v1/history/meal/{mealId}/last - Get last cook date for a meal
 *
 * ## Authentication
 * All endpoints require valid authentication. User ID is extracted from
 * Spring Security context via UserService.getCurrentUser().
 *
 * ## Response Format
 * Success responses include entities/lists in body with appropriate HTTP status codes.
 * Errors throw exceptions handled by global exception handlers.
 *
 * @property cookHistoryService Service for cook history operations
 * @property userService Service for retrieving current authenticated user
 *
 * @see MealCookHistoryService
 * @see MealCookHistoryDto
 */
@RestController
@RequestMapping("/api/v1/history")
class CookHistoryController(
    private val cookHistoryService: MealCookHistoryService,
    private val userService: UserService
) {

    /**
     * POST /api/v1/history/{mealId}/log
     *
     * Records a new cook history entry for the authenticated user.
     *
     * Creates a cook history entry with the specified meal and optional details.
     * If the user has a daily meal plan for today with this meal, it will be
     * automatically marked as completed.
     *
     * ## Path Parameters
     * - **mealId**: MongoDB ObjectId of the meal that was cooked
     *
     * ## Query Parameters
     * - **portionSize** (optional): Number of portions cooked
     * - **rating** (optional): User rating for this cook (1-5 scale)
     *
     * ## Request Body
     * - **notes** (optional): User notes about this cooking experience
     *
     * ## Response
     * - **200 OK**: Cook history entry created successfully
     * - **404 Not Found**: If meal with mealId does not exist
     *
     * ## Side Effects
     * - Creates MealCookHistory entry
     * - May complete today's DailyMealPlan if it exists for this meal
     *
     * ## Authentication Required
     * User must be authenticated. User ID is extracted from security context.
     *
     * @param mealId MongoDB ObjectId of the meal
     * @param portionSize Optional number of portions cooked
     * @param rating Optional rating (1-5) for this cook
     * @param notes Optional user notes
     * @return ResponseEntity with 200 OK status
     * @throws IdNotFoundException If meal does not exist
     */
    @PostMapping("/{mealId}/log")
    fun logMealCooked(
        @PathVariable mealId: String,
        @RequestParam(required = false) portionSize: Int?,
        @RequestParam(required = false) rating: Int?,
        @RequestBody(required = false) notes: String?
    ): ResponseEntity<Void> {
        val userId = userService.getCurrentUser().id!!.toString()

        cookHistoryService.recordMealCooked(
            userId = userId,
            mealId = mealId,
            portionSize = portionSize,
            rating = rating,
            notes = notes
        )

        return ResponseEntity.ok().build()
    }

    /**
     * GET /api/v1/history
     *
     * Retrieves paginated cook history for the authenticated user.
     *
     * Returns all cook history entries for the current user, sorted by cook date
     * descending (most recent first). Results are paginated for efficient loading.
     *
     * ## Query Parameters
     * - **page** (optional, default: 0): Zero-based page number
     * - **pageSize** (optional, default: 20): Number of entries per page
     *
     * ## Response
     * - **200 OK**: Page of [MealCookHistoryDto] objects
     *
     * ## Pagination Response Structure
     * Response includes:
     * - content: List of cook history DTOs for current page
     * - totalElements: Total number of history entries
     * - totalPages: Total number of pages
     * - number: Current page number
     * - size: Page size
     *
     * ## Authentication Required
     * User must be authenticated. User ID is extracted from security context.
     *
     * @param page Zero-based page number
     * @param pageSize Number of entries per page
     * @return ResponseEntity containing Page of cook history DTOs
     */
    @GetMapping
    fun getCookHistory(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<Page<MealCookHistoryDto>> {
        val userId = userService.getCurrentUser().id!!.toString()

        val history = cookHistoryService.getUserCookHistory(userId, page, pageSize)

        return ResponseEntity.ok(history)
    }

    /**
     * GET /api/v1/history/meal/{mealId}
     *
     * Retrieves cook history for a specific meal by the authenticated user.
     *
     * Returns all times the current user has cooked this specific meal, sorted by
     * date descending. Useful for showing "You've cooked this X times" and viewing
     * past ratings and notes.
     *
     * ## Path Parameters
     * - **mealId**: MongoDB ObjectId of the meal
     *
     * ## Query Parameters
     * - **page** (optional, default: 0): Zero-based page number
     * - **pageSize** (optional, default: 20): Number of entries per page
     *
     * ## Response
     * - **200 OK**: Page of [MealCookHistoryDto] objects for this meal
     *
     * ## Use Cases
     * - Display cooking statistics on meal detail page
     * - Show personal rating history for a meal
     * - View notes from previous cooks
     *
     * ## Authentication Required
     * User must be authenticated. User ID is extracted from security context.
     *
     * @param mealId MongoDB ObjectId of the meal
     * @param page Zero-based page number
     * @param pageSize Number of entries per page
     * @return ResponseEntity containing Page of meal-specific cook history
     */
    @GetMapping("/meal/{mealId}")
    fun getMealCookHistory(
        @PathVariable mealId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<Page<MealCookHistoryDto>> {
        val userId = userService.getCurrentUser().id!!.toString()

        val history = cookHistoryService.getMealSpecificHistory(userId, mealId, page, pageSize)

        return ResponseEntity.ok(history)
    }

    /**
     * GET /api/v1/history/recent
     *
     * Retrieves recent cook history entries for the authenticated user.
     *
     * Returns the most recent cook history entries up to the specified limit.
     * Used for activity feeds and quick history views.
     *
     * ## Query Parameters
     * - **limit** (optional, default: 10): Maximum number of entries to return
     *
     * ## Response
     * - **200 OK**: List of recent [MealCookHistoryDto] objects
     *
     * ## Use Cases
     * - Activity feed on dashboard
     * - "Recently Cooked" section
     * - Quick access to last few meals
     *
     * ## Authentication Required
     * User must be authenticated. User ID is extracted from security context.
     *
     * @param limit Maximum number of entries to return
     * @return ResponseEntity containing List of recent cook history
     */
    @GetMapping("/recent")
    fun getRecentHistory(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<MealCookHistoryDto>> {
        val userId = userService.getCurrentUser().id!!.toString()

        val history = cookHistoryService.getRecentHistory(userId, limit)

        return ResponseEntity.ok(history)
    }

    /**
     * GET /api/v1/history/meal/{mealId}/last
     *
     * Gets the last cook date for a specific meal by the authenticated user.
     *
     * Returns the timestamp when the user most recently cooked this meal.
     * Returns null if the user has never cooked this meal.
     *
     * ## Path Parameters
     * - **mealId**: MongoDB ObjectId of the meal
     *
     * ## Response
     * - **200 OK**: LocalDateTime of last cook, or null if never cooked
     *
     * ## Use Cases
     * - Display "Last cooked: 5 days ago" on meal detail page
     * - Calculate cooking frequency
     * - Meal recommendation algorithms
     *
     * ## Response Format
     * Returns ISO 8601 formatted timestamp string or null:
     * - "2024-01-15T18:30:00"
     * - null
     *
     * ## Authentication Required
     * User must be authenticated. User ID is extracted from security context.
     *
     * @param mealId MongoDB ObjectId of the meal
     * @return ResponseEntity containing last cook timestamp or null
     */
    @GetMapping("/meal/{mealId}/last")
    fun getLastCookDate(
        @PathVariable mealId: String
    ): ResponseEntity<LocalDateTime?> {
        val userId = userService.getCurrentUser().id!!.toString()

        val lastCookDate = cookHistoryService.getLastCookDateForMeal(userId, mealId)

        return ResponseEntity.ok(lastCookDate)
    }
}
