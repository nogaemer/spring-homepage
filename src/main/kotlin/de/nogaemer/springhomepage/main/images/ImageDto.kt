package de.nogaemer.springhomepage.main.images

/**
 * Data transfer object for image upload requests.
 *
 * Encapsulates the request body for the `/api/v1/images/upload` endpoint.
 *
 * ## Format
 * The base64 string should contain only the encoded image data without the
 * data URI prefix (e.g., without "data:image/jpeg;base64,").
 *
 * ## Example JSON
 * ```json
 * {
 *   "base64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
 * }
 * ```
 *
 * @property base64 Base64-encoded image data (without data URI prefix)
 */
data class ImageDto (
    var base64: String
)