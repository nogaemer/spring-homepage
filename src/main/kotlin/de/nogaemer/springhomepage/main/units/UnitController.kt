/**
 * REST API controller for ingredient unit management operations.
 *
 * Provides endpoints for searching and creating measurement units.
 * All endpoints are prefixed with /api/v1/units.
 */
package de.nogaemer.springhomepage.main.units

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller handling HTTP requests for unit operations.
 *
 * Units can be searched with intelligent relevance ranking across multiple
 * fields and created for use in ingredient definitions.
 *
 * @property service The unit service providing business logic
 */
@RestController
@RequestMapping("/api/v1/units")
class UnitController {
    @Autowired
    val service: UnitService? = null

    /**
     * Lists ingredient units with optional search and relevance ranking.
     *
     * Returns units sorted by relevance when query is provided, or by full name when empty.
     * Uses MongoDB aggregation to score matches across abbreviations, names, descriptions,
     * and categories, checking both singular and plural forms.
     *
     * **Route**: GET /api/v1/units
     *
     * **Query Parameters**:
     * - limit: Maximum number of results (default: 10)
     * - query: Search query for filtering (default: empty, returns all)
     *
     * **Search Scoring**:
     * - Abbreviation match (singular or plural): 4 points
     * - Full name match (singular or plural): 4 points
     * - Description match: 2 points
     * - Category match: 1 point
     *
     * **Response**: 200 OK
     * ```json
     * [
     *   {
     *     "id": "507f1f77bcf86cd799439011",
     *     "abbreviation": "g",
     *     "abbreviationPlural": "gs",
     *     "fullName": "gram",
     *     "fullNamePlural": "grams",
     *     "countable": false,
     *     "category": "weight",
     *     "description": "Metric unit of mass"
     *   }
     * ]
     * ```
     *
     * **Examples**:
     * - GET /api/v1/units?limit=20 → First 20 units alphabetically
     * - GET /api/v1/units?query=tea&limit=5 → Units matching "tea" (teaspoon, etc.)
     * - GET /api/v1/units?query=weight → Units in weight category
     *
     * @param limit Maximum number of results (default: 10)
     * @param query Search query for filtering by any field
     * @return ResponseEntity containing list of matching units
     */
    @GetMapping
    fun getUnits(
        @RequestParam(
            value = "limit",
            defaultValue = "10"
        ) limit: Int,
        @RequestParam(
            value = "query",
            defaultValue = ""
        ) query: String
    ): ResponseEntity<List<IngredientUnit>> {
        return ResponseEntity<List<IngredientUnit>>(service?.getUnits(limit,0, query), HttpStatus.OK)
    }

    /**
     * Creates or updates a single ingredient unit.
     *
     * If unit has no ID or ID doesn't exist, creates new unit with generated ID.
     * If unit has existing ID, updates that unit in the database.
     *
     * **Route**: POST /api/v1/units
     *
     * **Request Body**:
     * ```json
     * {
     *   "abbreviation": "tbsp",
     *   "abbreviationPlural": "tbsps",
     *   "fullName": "tablespoon",
     *   "fullNamePlural": "tablespoons",
     *   "countable": false,
     *   "category": "volume",
     *   "description": "US unit of volume equal to 3 teaspoons"
     * }
     * ```
     *
     * **Response**: 200 OK
     * ```json
     * {
     *   "id": "507f1f77bcf86cd799439011",
     *   "abbreviation": "tbsp",
     *   "abbreviationPlural": "tbsps",
     *   "fullName": "tablespoon",
     *   "fullNamePlural": "tablespoons",
     *   "countable": false,
     *   "category": "volume",
     *   "description": "US unit of volume equal to 3 teaspoons"
     * }
     * ```
     *
     * **Validation**:
     * - All fields except id are required
     *
     * **Error Responses**:
     * - 400 BAD_REQUEST: Missing required fields
     *
     * @param unit The unit data to save
     * @return ResponseEntity containing the saved unit with generated/existing ID
     */
    @PostMapping
    fun createUnit(
        @RequestBody unit: IngredientUnit
    ): ResponseEntity<IngredientUnit>{
        return ResponseEntity<IngredientUnit>(service?.saveUnit(unit), HttpStatus.OK)
    }

}