/**
 * REST API controller for note management operations.
 *
 * Provides endpoints for retrieving, creating, and deleting notes on meals.
 * All endpoints are prefixed with /api/v1/notes and require JWT authentication.
 */
package de.nogaemer.springhomepage.main.notes

import de.nogaemer.springhomepage.security.config.JwtService
import jakarta.servlet.http.HttpServletRequest
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller handling HTTP requests for note operations.
 *
 * All write operations require authentication via JWT token in request header.
 * Notes are automatically associated with the authenticated user.
 *
 * @property jwtService Service for extracting user from JWT tokens
 * @property service The note service providing business logic
 */
@RestController
@RequestMapping("/api/v1/notes")
class NoteController(
    val jwtService: JwtService,
    val service: NoteService
) {

    /**
     * Retrieves all notes for a specific meal with user information.
     *
     * Returns notes enriched with author details (user ID and username).
     * No authentication required for reading notes.
     *
     * **Route**: GET /api/v1/notes/{id}
     *
     * **Path Parameters**:
     * - id: Meal ObjectId as string
     *
     * **Response**: 200 OK
     * ```json
     * [
     *   {
     *     "note": {
     *       "id": "507f1f77bcf86cd799439011",
     *       "mealId": "507f1f77bcf86cd799439012",
     *       "userId": "507f1f77bcf86cd799439013",
     *       "note": "Added extra garlic - delicious!",
     *       "date": "2024-01-15T10:30:00",
     *       "modifiedDate": "2024-01-15T10:30:00"
     *     },
     *     "user": {
     *       "id": "507f1f77bcf86cd799439013",
     *       "username": "john_doe"
     *     }
     *   }
     * ]
     * ```
     *
     * @param id The meal ID to get notes for
     * @return ResponseEntity containing list of notes with user details
     */
    @GetMapping("/{id}")
    fun getNote(
        @PathVariable id: String
    ): ResponseEntity<List<NoteResponse>> {
        return ResponseEntity.ok(service.getNotesByMealId(ObjectId(id)))
    }

    /**
     * Creates a new note for a meal.
     *
     * Authenticated user is automatically set as the note author.
     * Users can only have one note per meal (enforced by service layer).
     *
     * **Route**: POST /api/v1/notes
     *
     * **Headers**: Authorization: Bearer {jwt_token}
     *
     * **Request Body**:
     * ```json
     * {
     *   "mealId": "507f1f77bcf86cd799439012",
     *   "note": "Substituted butter with olive oil"
     * }
     * ```
     *
     * **Response**: 201 CREATED
     * ```json
     * {
     *   "id": "507f1f77bcf86cd799439011",
     *   "mealId": "507f1f77bcf86cd799439012",
     *   "userId": "507f1f77bcf86cd799439013",
     *   "note": "Substituted butter with olive oil",
     *   "date": "2024-01-15T10:30:00",
     *   "modifiedDate": "2024-01-15T10:30:00"
     * }
     * ```
     *
     * **Error Responses**:
     * - 404 NOT_FOUND: Meal doesn't exist
     * - 409 CONFLICT: User already has a note for this meal
     * - 401 UNAUTHORIZED: Invalid or missing JWT token
     *
     * @param note The note data (userId will be overridden with authenticated user)
     * @param request HTTP request containing JWT token in Authorization header
     * @return ResponseEntity containing the created note with generated ID
     * @throws RuntimeException If user cannot be extracted from JWT token
     */
    @PostMapping
    fun createNote(
        @RequestBody note: Note,
        request: HttpServletRequest
    ): ResponseEntity<Note> {
        val user = jwtService.extractUserFromRequest(request)
            ?: throw RuntimeException("Note not found")

        note.userId = user.id!!
        return ResponseEntity<Note>(service.create(note), HttpStatus.CREATED)
    }

    /**
     * Deletes a note by ID.
     *
     * Only the note author or users with ADMIN role can delete notes.
     * Automatically removes the note reference from the meal document.
     *
     * **Route**: DELETE /api/v1/notes/{id}
     *
     * **Headers**: Authorization: Bearer {jwt_token}
     *
     * **Path Parameters**:
     * - id: Note ObjectId as string
     *
     * **Response**: 200 OK (empty body)
     *
     * **Error Responses**:
     * - 404 NOT_FOUND: Note doesn't exist
     * - 403 FORBIDDEN: User is not the note author or admin
     * - 401 UNAUTHORIZED: Invalid or missing JWT token
     *
     * @param id The note ID to delete
     * @return ResponseEntity with 200 OK status
     */
    @DeleteMapping("/{id}")
    fun deleteNote(
        @PathVariable id: String
    ): ResponseEntity<*> {
        service.delete(ObjectId(id))
        return ResponseEntity.ok().build<String>()
    }
}
