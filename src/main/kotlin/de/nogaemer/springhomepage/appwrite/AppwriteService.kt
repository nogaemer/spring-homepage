package de.nogaemer.springhomepage.appwrite

import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Role
import io.appwrite.extensions.toJson
import io.appwrite.models.InputFile
import io.appwrite.services.Storage
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files

/**
 * Service for interacting with Appwrite storage using a dual-path strategy.
 *
 * This service provides a fallback mechanism for Appwrite storage operations:
 * 1. **Primary path**: Uses the Appwrite Kotlin SDK (suspend functions wrapped with [runBlocking])
 * 2. **Fallback path**: Uses direct HTTP calls via OkHttp when SDK is unavailable or fails
 *
 * The SDK path is preferred because it provides better type safety and handles authentication
 * automatically. However, if the SDK client cannot be initialized or an SDK call fails,
 * the service gracefully falls back to HTTP-based operations.
 *
 * ## Threading considerations with runBlocking
 * The Appwrite SDK uses suspend functions (coroutines) for asynchronous operations.
 * Since this service is called from synchronous Spring controller/service code,
 * [runBlocking] is used to bridge the gap. This blocks the calling thread until the
 * coroutine completes, which is acceptable for I/O operations in a typical Spring application
 * with thread-per-request model. For high-throughput applications, consider making the
 * entire call chain suspending.
 *
 * @property endpoint The Appwrite API endpoint URL (e.g., "https://cloud.appwrite.io/v1")
 * @property projectId The Appwrite project identifier
 * @property apiKey The API key for server-side authentication
 * @property bucketId The storage bucket ID where files are stored
 */
@Service
class AppwriteService(
    @param:Value("\${appwrite.endpoint:}") private val endpoint: String,
    @param:Value("\${appwrite.projectId:}") private val projectId: String,
    @param:Value("\${appwrite.apiKey:}") private val apiKey: String,
    @param:Value("\${appwrite.bucketId:}") private val bucketId: String
) {

    private val client = OkHttpClient()
    private val log = LoggerFactory.getLogger(AppwriteService::class.java)

    /**
     * Appwrite SDK client instance, initialized only when all required configuration is present.
     * If initialization fails or configuration is missing, this will be null and HTTP fallback is used.
     */
    private val awClient: Client? = if (endpoint.isNotBlank() && projectId.isNotBlank() && apiKey.isNotBlank()) {
        try {
            Client().setEndpoint(endpoint).setProject(projectId).setKey(apiKey)
        } catch (e: Exception) {
            log.warn("Failed to initialize Appwrite SDK client: {}", e.message)
            null
        }
    } else null

    /**
     * Appwrite Storage service instance derived from [awClient].
     * Null if SDK client initialization failed.
     */
    private val storageSdk: Storage? = awClient?.let { Storage(it) }

    /**
     * Metadata about a file stored in Appwrite storage.
     *
     * @property id Unique file identifier (used for subsequent operations)
     * @property bucketId The bucket where the file is stored
     * @property name Original filename
     * @property size File size in bytes
     * @property mimeType Content type of the file
     */
    data class AppwriteFile(
        val id: String,
        val bucketId: String,
        val name: String,
        val size: Long,
        val mimeType: String
    )

    /**
     * Uploads an image file to Appwrite storage from a [MultipartFile].
     *
     * This method delegates to the bytes-based overload for actual upload processing.
     *
     * ## Strategy
     * 1. **SDK path**: Uses [runBlocking] to call suspend [Storage.createFile]
     * 2. **Fallback path**: Uses direct HTTP POST to `/storage/buckets/{bucketId}/files`
     *
     * @param file The multipart file from an HTTP upload request
     * @param fileId Optional file ID; if provided, Appwrite attempts to use this ID (useful for updates)
     * @return [AppwriteFile] metadata about the uploaded file
     * @throws IOException if upload fails via both SDK and HTTP paths
     */
    @Throws(IOException::class)
    fun uploadImage(file: MultipartFile, fileId: String? = null): AppwriteFile {
        // Delegate to bytes overload
        return uploadImage(file.bytes, file.originalFilename ?: "file.jpg", file.contentType, fileId)
    }

    /**
     * Uploads an image file to Appwrite storage from raw byte array.
     *
     * This is the primary upload method, useful when working with processed images
     * (e.g., from [java.awt.image.BufferedImage]).
     *
     * ## Upload flow (SDK path)
     * 1. Create a temporary file using [Files.createTempFile]
     * 2. Write the byte array to the temp file
     * 3. Create an [InputFile] from the temp file path (required by SDK)
     * 4. Call [Storage.createFile] with bucket ID, file ID, input file, and permissions
     * 5. Set permissions to [Permission.update] with [Role.any()] for public access
     * 6. Parse the response JSON to extract file metadata
     * 7. Delete the temporary file in a finally block
     *
     * ## Upload flow (HTTP fallback path)
     * 1. Build a multipart/form-data request with the file bytes
     * 2. Add optional fileId as a form field
     * 3. POST to `/storage/buckets/{bucketId}/files` with auth headers
     * 4. Parse response JSON to extract file metadata
     *
     * ## Threading implications
     * The SDK path uses [runBlocking], which blocks the calling thread until the
     * suspend function completes. This is necessary because Spring controller methods
     * are synchronous and cannot directly call suspend functions.
     *
     * @param bytes Raw image data as byte array
     * @param filename Desired filename (default: "file.jpg")
     * @param contentType MIME type (default: "image/jpeg")
     * @param fileId Optional file ID for deterministic naming or updates
     * @return [AppwriteFile] metadata about the uploaded file
     * @throws IOException if both SDK and HTTP upload attempts fail
     * @throws IllegalStateException if Appwrite configuration is not set (HTTP path only)
     */
    @Throws(IOException::class)
    fun uploadImage(bytes: ByteArray, filename: String = "file.jpg", contentType: String? = null, fileId: String? = null): AppwriteFile {
        if (storageSdk != null) {
            try {
                return runBlocking {
                    val tmp = Files.createTempFile("appwrite-upload-", "-${filename}").toFile()
                    tmp.writeBytes(bytes)
                    try {
                        val inputFile = InputFile.fromPath(tmp.absolutePath)
                        val createdId = fileId ?: ID.unique()
                        val storageFile = storageSdk.createFile(
                            bucketId = bucketId,
                            fileId = createdId,
                            file = inputFile,
                            permissions = listOf(
                                Permission.update(Role.any())
                            )
                        )

                        // parse result via toJson() extension into JSONObject
                        val json = JSONObject(storageFile.toJson())
                        val parsedId = json.optString("id", createdId)

                        AppwriteFile(
                            id = parsedId,
                            bucketId = json.optString("bucketId", bucketId),
                            name = json.optString("name", filename),
                            size = json.optLong("size", bytes.size.toLong()),
                            mimeType = json.optString("mimeType", contentType ?: "image/jpeg")
                        )
                    } finally {
                        tmp.delete()
                    }
                }
            } catch (e: Throwable) {
                log.warn("Appwrite SDK upload failed, falling back to HTTP client: {}", e.message)
            }
        }

        if (endpoint.isBlank() || projectId.isBlank() || apiKey.isBlank() || bucketId.isBlank()) {
            throw IllegalStateException("Appwrite configuration is not set")
        }

        val url = "$endpoint/storage/buckets/$bucketId/files"
        val ct = contentType ?: "image/jpeg"
        val fileBody = bytes.toRequestBody(ct.toMediaTypeOrNull())

        val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", filename, fileBody)

        if (!fileId.isNullOrBlank()) {
            multipartBuilder.addFormDataPart("fileId", fileId)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("X-Appwrite-Project", projectId)
            .addHeader("X-Appwrite-Key", apiKey)
            .post(multipartBuilder.build())
            .build()

        client.newCall(request).execute().use { resp ->
            val bodyString = resp.body?.string()
            if (!resp.isSuccessful) {
                log.error("Appwrite upload (HTTP) failed: code={} body={}", resp.code, bodyString)
                throw IOException("Appwrite upload failed with HTTP ${resp.code}: $bodyString")
            }
            val json = if (bodyString.isNullOrBlank()) JSONObject() else JSONObject(bodyString)
            val parsedId = json.optString("id", fileId ?: "")
            return AppwriteFile(
                id = parsedId,
                bucketId = json.optString("bucketId", bucketId),
                name = json.optString("name", filename),
                size = json.optLong("size", bytes.size.toLong()),
                mimeType = json.optString("mimeType", ct)
            )
        }
    }

    /**
     * Retrieves metadata for a file stored in Appwrite.
     *
     * ## Strategy
     * 1. **SDK path**: Uses [runBlocking] to call [Storage.getFile], parses response JSON
     * 2. **Fallback path**: HTTP GET to `/storage/buckets/{bucketId}/files/{fileId}`
     *
     * @param fileId The unique identifier of the file
     * @return [AppwriteFile] metadata containing id, bucket, name, size, and MIME type
     * @throws IOException if both SDK and HTTP requests fail
     * @throws IllegalStateException if Appwrite configuration is not set (HTTP path only)
     */
    @Throws(IOException::class)
    fun getFileMetadata(fileId: String): AppwriteFile {
        if (storageSdk != null) {
            try {
                return runBlocking {
                    val sdkResult = storageSdk.getFile(bucketId, fileId)
                    val json = JSONObject(sdkResult.toJson())
                    val parsedId = json.optString("id", fileId)
                    AppwriteFile(
                        id = parsedId,
                        bucketId = json.optString("bucketId", bucketId),
                        name = json.optString("name", ""),
                        size = json.optLong("size", 0L),
                        mimeType = json.optString("mimeType", "application/octet-stream")
                    )
                }
            } catch (e: Throwable) {
                log.warn("Appwrite SDK getFile failed, falling back to HTTP: {}", e.message)
            }
        }

        if (endpoint.isBlank() || projectId.isBlank() || apiKey.isBlank() || bucketId.isBlank()) {
            throw IllegalStateException("Appwrite configuration is not set")
        }

        val url = "$endpoint/storage/buckets/$bucketId/files/$fileId"
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Appwrite-Project", projectId)
            .addHeader("X-Appwrite-Key", apiKey)
            .get()
            .build()

        client.newCall(request).execute().use { resp ->
            val bodyString = resp.body?.string()
            if (!resp.isSuccessful) {
                log.error("Appwrite metadata failed: code={} body={}", resp.code, bodyString)
                throw IOException("Appwrite metadata failed with HTTP ${resp.code}: $bodyString")
            }
            val json = JSONObject(if (bodyString.isNullOrBlank()) "{}" else bodyString)
            val parsedId = json.optString("id", fileId)
            return AppwriteFile(
                id = parsedId,
                bucketId = json.optString("bucketId", bucketId),
                name = json.optString("name", ""),
                size = json.optLong("size", 0L),
                mimeType = json.optString("mimeType", "application/octet-stream")
            )
        }
    }

    /**
     * Downloads the raw bytes of a file from Appwrite storage.
     *
     * Returns both the file content and its MIME type for proper HTTP response handling.
     *
     * ## Strategy
     * 1. **SDK path**: Uses [runBlocking] to call [Storage.getFileDownload], returns raw bytes
     * 2. **Fallback path**: HTTP GET to `/storage/buckets/{bucketId}/files/{fileId}/download`
     *
     * @param fileId The unique identifier of the file to download
     * @return Pair of [ByteArrayResource] containing file bytes and String content type
     * @throws IOException if both SDK and HTTP download attempts fail
     * @throws IllegalStateException if Appwrite configuration is not set (HTTP path only)
     */
    @Throws(IOException::class)
    fun downloadImage(fileId: String): Pair<ByteArrayResource, String> {
        if (storageSdk != null) {
            try {
                return runBlocking {
                    val bytes = storageSdk.getFileDownload(bucketId, fileId)
                    Pair(ByteArrayResource(bytes), "application/octet-stream")
                }
            } catch (e: Throwable) {
                log.warn("Appwrite SDK download failed, falling back to HTTP: {}", e.message)
            }
        }

        if (endpoint.isBlank() || projectId.isBlank() || apiKey.isBlank() || bucketId.isBlank()) {
            throw IllegalStateException("Appwrite configuration is not set")
        }

        val url = "$endpoint/storage/buckets/$bucketId/files/$fileId/download"
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Appwrite-Project", projectId)
            .addHeader("X-Appwrite-Key", apiKey)
            .get()
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                val bodyString = resp.body?.string()
                log.error("Appwrite download failed: code={} body={}", resp.code, bodyString)
                throw IOException("Appwrite download failed with HTTP ${resp.code}: $bodyString")
            }
            val bytes = resp.body?.bytes() ?: ByteArray(0)
            val contentType = resp.header("Content-Type") ?: "application/octet-stream"
            return Pair(ByteArrayResource(bytes), contentType)
        }
    }

    /**
     * Requests an on-the-fly image preview from Appwrite with optional resizing.
     *
     * The Appwrite preview endpoint can generate resized images dynamically without
     * storing multiple copies. This is useful for responsive images.
     *
     * **Note**: This method uses HTTP only (no SDK path) because the SDK's preview
     * functionality requires additional setup. If the preview endpoint is not supported
     * or returns an error, this method automatically falls back to [downloadImage].
     *
     * @param fileId The unique identifier of the file
     * @param width Optional width in pixels for the preview
     * @param height Optional height in pixels for the preview
     * @return Pair of [ByteArrayResource] containing preview bytes and String content type
     * @throws IOException if HTTP request fails and download fallback also fails
     * @throws IllegalStateException if Appwrite configuration is not set
     */
    @Throws(IOException::class)
    fun previewImage(fileId: String, width: Int? = null, height: Int? = null): Pair<ByteArrayResource, String> {
        if (endpoint.isBlank() || projectId.isBlank() || apiKey.isBlank() || bucketId.isBlank()) {
            throw IllegalStateException("Appwrite configuration is not set")
        }

        val query = mutableListOf<String>()
        width?.let { query.add("width=$it") }
        height?.let { query.add("height=$it") }
        val q = if (query.isEmpty()) "" else "?${query.joinToString("&") }"
        val url = "$endpoint/storage/buckets/$bucketId/files/$fileId/preview$q"

        val request = Request.Builder()
            .url(url)
            .addHeader("X-Appwrite-Project", projectId)
            .addHeader("X-Appwrite-Key", apiKey)
            .get()
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                log.warn("Preview endpoint failed with code=${resp.code}, falling back to download")
                return downloadImage(fileId)
            }
            val bytes = resp.body?.bytes() ?: ByteArray(0)
            val contentType = resp.header("Content-Type") ?: "image/jpeg"
            return Pair(ByteArrayResource(bytes), contentType)
        }
    }

    /**
     * Deletes a file from Appwrite storage.
     *
     * ## Strategy
     * 1. **SDK path**: Uses [runBlocking] to call [Storage.deleteFile]
     * 2. **Fallback path**: HTTP DELETE to `/storage/buckets/{bucketId}/files/{fileId}`
     *
     * @param fileId The unique identifier of the file to delete
     * @return true if deletion was successful
     * @throws IOException if both SDK and HTTP delete attempts fail
     * @throws IllegalStateException if Appwrite configuration is not set (HTTP path only)
     */
    @Throws(IOException::class)
    fun deleteImage(fileId: String): Boolean {
        if (storageSdk != null) {
            try {
                runBlocking {
                    storageSdk.deleteFile(bucketId, fileId)
                }
                return true
            } catch (e: Throwable) {
                log.warn("Appwrite SDK delete failed, falling back to HTTP: {}", e.message)
            }
        }

        if (endpoint.isBlank() || projectId.isBlank() || apiKey.isBlank() || bucketId.isBlank()) {
            throw IllegalStateException("Appwrite configuration is not set")
        }

        val url = "$endpoint/storage/buckets/$bucketId/files/$fileId"
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Appwrite-Project", projectId)
            .addHeader("X-Appwrite-Key", apiKey)
            .delete()
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                val bodyString = resp.body?.string()
                log.error("Appwrite delete failed: code={} body={}", resp.code, bodyString)
                throw IOException("Appwrite delete failed with HTTP ${resp.code}: $bodyString")
            }
            return true
        }
    }

    /**
     * Constructs a public URL for previewing a file stored in Appwrite.
     *
     * This URL can be used directly in HTML img src or srcset attributes.
     * The preview endpoint supports on-the-fly resizing via query parameters.
     *
     * ## URL format
     * Without width: `{endpoint}/storage/buckets/{bucketId}/files/{fileId}/preview?project={projectId}`
     * With width: `{endpoint}/storage/buckets/{bucketId}/files/{fileId}/preview?project={projectId}&width={width}`
     *
     * @param fileId The unique identifier of the file
     * @param width Optional width in pixels for responsive images (e.g., 360, 640, 1080)
     * @return Complete URL string for the preview endpoint
     */
    fun getFilePreviewUrl(fileId: String, width: Int? = null): String {
        val p = "?project=$projectId"
        val q = if (width == null) "" else "&width=$width"
        return "$endpoint/storage/buckets/$bucketId/files/$fileId/preview$p$q"
    }

    /**
     * Constructs a public URL for directly downloading a file from Appwrite.
     *
     * This URL triggers a browser download with the original filename and content type.
     *
     * ## URL format
     * `{endpoint}/storage/buckets/{bucketId}/files/{fileId}/download?project={projectId}`
     *
     * @param fileId The unique identifier of the file
     * @return Complete URL string for the download endpoint
     */
    fun getFileDownloadUrl(fileId: String): String {
        val p = "?project=$projectId"
        return "$endpoint/storage/buckets/$bucketId/files/$fileId/download$p"
    }

}

