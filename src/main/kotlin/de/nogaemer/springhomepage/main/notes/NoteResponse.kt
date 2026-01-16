/**
 * Data transfer object for note responses with user information.
 *
 * Enriches note data with author details for API responses.
 * Used when displaying notes to show both the content and who wrote it.
 */
package de.nogaemer.springhomepage.main.notes

import de.nogaemer.springhomepage.user.UserResponse

/**
 * Response wrapper combining note data with user information.
 *
 * This DTO is used in REST API responses to provide complete note context
 * including the author's identity. Simplifies client-side rendering by
 * avoiding additional user lookup requests.
 *
 * @property note The complete note entity with all fields
 * @property user The note author's information (ID and username)
 */
data class NoteResponse(
    val note: Note,
    val user: UserResponse
)