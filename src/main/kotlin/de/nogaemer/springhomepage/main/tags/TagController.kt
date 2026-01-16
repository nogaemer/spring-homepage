/**
 * REST API controller for tag management operations.
 *
 * Provides endpoints for searching and creating tags.
 * All endpoints are prefixed with /api/v1/tags.
 */
package de.nogaemer.springhomepage.main.tags

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller handling HTTP requests for tag operations.
 *
 * Tags can be searched with intelligent relevance ranking and created
 * for use in meal categorization.
 *
 * @property service The tag service providing business logic
 */
@RestController
@RequestMapping("/api/v1/tags")
class TagController {
    @Autowired
    val service: TagService? = null

    /**
     * Lists tags with optional search and relevance ranking.
     *
     * Returns tags sorted by relevance when query is provided, or by name when empty.
     * Uses MongoDB aggregation to score matches across name, type, and description.
     *
     * **Route**: GET /api/v1/tags
     *
     * **Query Parameters**:
     * - limit: Maximum number of results (default: 10)
     * - query: Search query for filtering (default: empty, returns all)
     *
     * **Search Scoring**:
     * - Name match: 3 points
     * - Description match: 2 points
     * - Type match: 1 point
     *
     * **Response**: 200 OK
     * ```json
     * [
     *   {
     *     "id": "507f1f77bcf86cd799439011",
     *     "name": "Vegetarian",
     *     "type": "diet",
     *     "description": "Contains no meat or fish",
     *     "color": "#4CAF50"
     *   }
     * ]
     * ```
     *
     * **Examples**:
     * - GET /api/v1/tags?limit=20 → First 20 tags alphabetically
     * - GET /api/v1/tags?query=vegan&limit=5 → Top 5 vegan-related tags
     *
     * @param limit Maximum number of results (default: 10)
     * @param query Search query for filtering by name, type, or description
     * @return ResponseEntity containing list of matching tags
     */
    @GetMapping
    fun getTags(
        @RequestParam(
            value = "limit",
            defaultValue = "10"
        ) limit: Int,
        @RequestParam(
            value = "query",
            defaultValue = ""
        ) query: String
    ): ResponseEntity<List<Tag>> {
        return ResponseEntity<List<Tag>>(service?.getTags(limit,0, query), HttpStatus.OK)
    }

    /**
     * Creates or updates a single tag.
     *
     * If tag has no ID or ID doesn't exist, creates new tag with generated ID.
     * If tag has existing ID, updates that tag in the database.
     *
     * **Route**: POST /api/v1/tags
     *
     * **Request Body**:
     * ```json
     * {
     *   "name": "Gluten Free",
     *   "type": "diet",
     *   "description": "Contains no gluten or wheat products",
     *   "color": "#FFB74D"
     * }
     * ```
     *
     * **Response**: 200 OK
     * ```json
     * {
     *   "id": "507f1f77bcf86cd799439011",
     *   "name": "Gluten Free",
     *   "type": "diet",
     *   "description": "Contains no gluten or wheat products",
     *   "color": "#FFB74D"
     * }
     * ```
     *
     * **Validation**:
     * - color: Must match pattern ^#([A-Fa-f0-9]{6})$ (e.g., #FF5733)
     * - All fields except id are required
     *
     * **Error Responses**:
     * - 400 BAD_REQUEST: Invalid color format or missing required fields
     *
     * @param tag The tag data to save
     * @return ResponseEntity containing the saved tag with generated/existing ID
     */
    @PostMapping
    fun createTag(
        @RequestBody tag: Tag
    ): ResponseEntity<Tag>{
        return ResponseEntity<Tag>(service?.saveTag(tag), HttpStatus.OK)
    }

}