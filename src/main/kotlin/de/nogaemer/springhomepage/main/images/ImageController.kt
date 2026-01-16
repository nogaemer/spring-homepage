package de.nogaemer.springhomepage.main.images

import org.json.JSONObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for image upload operations.
 *
 * Provides HTTP endpoints for uploading and processing images with automatic
 * cropping to 16:9 aspect ratio and responsive URL generation.
 *
 * ## Base path
 * All endpoints are mapped under `/api/v1/images`
 *
 * @property imageService Service for processing and uploading images
 */
@RestController
@RequestMapping("/api/v1/images")
class ImageController(
    val imageService: ImageService
) {

    /**
     * Uploads and processes an image from base64-encoded data.
     *
     * ## Endpoint
     * `POST /api/v1/images/upload`
     *
     * ## Request format
     * Expects JSON body with structure defined by [ImageDto]:
     * ```json
     * {
     *   "base64": "iVBORw0KGgoAAAANSUhEUgAA..."
     * }
     * ```
     *
     * ## Processing steps
     * 1. Decodes base64 string to image bytes
     * 2. Crops image to 16:9 aspect ratio
     * 3. Uploads to Appwrite storage
     * 4. Generates responsive preview URLs at multiple resolutions
     *
     * ## Response format
     * Returns [Image] object as JSON:
     * ```json
     * {
     *   "thumbnail": "https://...?width=200",
     *   "srcSetArray": ["https://...?width=360", "https://...?width=640", ...],
     *   "srcSetString": "https://...?width=360 360w, https://...?width=640 640w, ...",
     *   "deleteUrls": ["file-id-1"]
     * }
     * ```
     *
     * ## HTTP status codes
     * - **200 OK**: Image successfully uploaded and processed
     * - **400 Bad Request**: Invalid base64 data or malformed request
     * - **500 Internal Server Error**: Upload failed or processing error
     *
     * @param imageDto Request body containing base64-encoded image
     * @return ResponseEntity containing processed [Image] with preview URLs
     */
     @PostMapping("/upload")
     fun uploadImage(
         @RequestBody imageDto: ImageDto
     ): ResponseEntity<Image> {
         return ResponseEntity.ok(imageService.uploadImage(imageDto.base64))
     }


}