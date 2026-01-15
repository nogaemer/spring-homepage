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

@Service
class AppwriteService(
    @param:Value("\${appwrite.endpoint:}") private val endpoint: String,
    @param:Value("\${appwrite.projectId:}") private val projectId: String,
    @param:Value("\${appwrite.apiKey:}") private val apiKey: String,
    @param:Value("\${appwrite.bucketId:}") private val bucketId: String
) {

    private val client = OkHttpClient()
    private val log = LoggerFactory.getLogger(AppwriteService::class.java)

    // Initialize Appwrite SDK client & storage when config is present
    private val awClient: Client? = if (endpoint.isNotBlank() && projectId.isNotBlank() && apiKey.isNotBlank()) {
        try {
            Client().setEndpoint(endpoint).setProject(projectId).setKey(apiKey)
        } catch (e: Exception) {
            log.warn("Failed to initialize Appwrite SDK client: {}", e.message)
            null
        }
    } else null

    private val storageSdk: Storage? = awClient?.let { Storage(it) }

    data class AppwriteFile(
        val id: String,
        val bucketId: String,
        val name: String,
        val size: Long,
        val mimeType: String
    )

    /**
     * Uploads an image file to Appwrite storage. Returns metadata about the stored file.
     * If fileId is provided Appwrite will attempt to use that id (useful for overwriting).
     */
    @Throws(IOException::class)
    fun uploadImage(file: MultipartFile, fileId: String? = null): AppwriteFile {
        // Delegate to bytes overload
        return uploadImage(file.bytes, file.originalFilename ?: "file.jpg", file.contentType, fileId)
    }

    // New overload: upload from raw bytes (useful when you have a BufferedImage)
    @Throws(IOException::class)
    fun uploadImage(bytes: ByteArray, filename: String = "file.jpg", contentType: String? = null, fileId: String? = null): AppwriteFile {
        // Prefer Appwrite SDK if available
        if (storageSdk != null) {
            try {
                return runBlocking {
                    // write bytes to a temp file and use InputFile.fromPath
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
                // If SDK call fails for any reason, fall back to HTTP implementation
                log.warn("Appwrite SDK upload failed, falling back to HTTP client: {}", e.message)
            }
        }

        // Fallback: existing HTTP-based upload
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
     * Returns metadata for a file as AppwriteFile
     */
    @Throws(IOException::class)
    fun getFileMetadata(fileId: String): AppwriteFile {
        // Try SDK first
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
     * Downloads the raw bytes of the file. Caller receives a ByteArrayResource and the content type.
     */
    @Throws(IOException::class)
    fun downloadImage(fileId: String): Pair<ByteArrayResource, String> {
        // Try SDK first
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
     * Requests an on-the-fly preview from Appwrite. Many Appwrite deployments support `/preview` endpoint.
     * If preview is not supported, this method falls back to `downloadImage`.
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
     * Deletes a file from Appwrite storage. Returns true on success.
     */
    @Throws(IOException::class)
    fun deleteImage(fileId: String): Boolean {
        // Try SDK first
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
     * Construct a public preview/view URL for a file stored in Appwrite.
     * If width is provided, returns the preview endpoint with width query param.
     */
    fun getFilePreviewUrl(fileId: String, width: Int? = null): String {
        val p = "?project=$projectId"
        val q = if (width == null) "" else "&width=$width"
        return "$endpoint/storage/buckets/$bucketId/files/$fileId/preview$p$q"
    }

    /**
     * Construct a direct download URL for a file.
     */
    fun getFileDownloadUrl(fileId: String): String {
        val p = "?project=$projectId"
        return "$endpoint/storage/buckets/$bucketId/files/$fileId/download$p"
    }

}

