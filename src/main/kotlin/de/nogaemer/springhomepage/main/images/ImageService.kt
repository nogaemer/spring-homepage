package de.nogaemer.springhomepage.main.images

import de.nogaemer.springhomepage.appwrite.AppwriteService
import de.nogaemer.springhomepage.utils.EnvUtils
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

/**
 * Service for processing and uploading images with automatic cropping and responsive sizing.
 *
 * This service handles the complete image upload pipeline:
 * 1. Decodes base64-encoded image data
 * 2. Reads the image into a [BufferedImage] for manipulation
 * 3. Crops the image to 16:9 aspect ratio (Full HD/widescreen format)
 * 4. Uploads the processed image to Appwrite storage
 * 5. Generates preview URLs at multiple resolutions for responsive web design
 *
 * The service integrates with [AppwriteService] for cloud storage and creates
 * srcset-compatible URL collections for optimal image delivery across devices.
 *
 * @property appwriteService Service for uploading to and managing Appwrite storage
 */
@Service
class ImageService(
    private val appwriteService: AppwriteService
) {
    /**
     * Processes and uploads a base64-encoded image, creating responsive preview URLs.
     *
     * ## Processing pipeline
     * 1. **Base64 decoding**: Converts base64 string to raw byte array using [Base64.getDecoder]
     * 2. **BufferedImage creation**: Reads bytes into a [BufferedImage] for manipulation
     * 3. **Aspect ratio cropping**: Crops to 16:9 using [cropToFullHd]
     * 4. **JPEG conversion**: Converts the [BufferedImage] to JPEG byte array
     * 5. **Appwrite upload**: Uploads processed image via [AppwriteService]
     * 6. **URL generation**: Creates preview URLs at multiple resolutions
     *
     * ## Resolution strategy
     * Generates preview URLs at four breakpoints for responsive design:
     * - **360w**: Mobile portrait (thumbnail/preview quality)
     * - **640w**: Mobile landscape / small tablets
     * - **820w**: Tablets / small desktop
     * - **1080w**: Full HD desktop displays
     *
     * Additionally creates a 200px thumbnail for gallery views.
     *
     * ## Appwrite integration
     * The uploaded image is stored once, and Appwrite's preview endpoint dynamically
     * generates resized versions on-demand using the width query parameter.
     *
     * @param base64Input Base64-encoded image string (without data URI prefix)
     * @return [Image] object containing thumbnail, srcSet array/string, and file ID for deletion
     * @throws IOException if image cannot be decoded, read, processed, or uploaded
     */
    fun uploadImage(base64Input: String): Image {
        val imageBytes = Base64.getDecoder().decode(base64Input)

        val inputStream = ByteArrayInputStream(imageBytes)
        val originalImage = ImageIO.read(inputStream) ?: throw IOException("Failed to read image from input stream")
        inputStream.close()

        val croppedImage = cropToFullHd(originalImage)

        val byteImage = bufferedImageToJpegBytes(croppedImage)

        val uploadedImage = appwriteService.uploadImage(byteImage, "image-800.jpg")

        val image360Url = appwriteService.getFilePreviewUrl(uploadedImage.id, 360)
        val image640Url = appwriteService.getFilePreviewUrl(uploadedImage.id, 640)
        val image820Url = appwriteService.getFilePreviewUrl(uploadedImage.id, 820)
        val imageFullUrl = appwriteService.getFilePreviewUrl(uploadedImage.id, 1080)

        return Image(
            thumbnail = appwriteService.getFilePreviewUrl(uploadedImage.id, 200),
            srcSetArray = arrayListOf(
                image360Url,
                image640Url,
                image820Url,
                imageFullUrl
            ),
            srcSetString = image360Url + " 360w, " +
                    image640Url + " 640w, " +
                    image820Url + " 800w, "+
                    imageFullUrl + " 1080w",
            deleteUrls = arrayOf(uploadedImage.id),
        )
    }

    /**
     * Converts a [BufferedImage] to JPEG format byte array.
     *
     * Uses [ImageIO.write] to serialize the image in JPEG format with default
     * compression settings.
     *
     * @param image The BufferedImage to convert
     * @return Byte array containing JPEG-encoded image data
     */
    private fun bufferedImageToJpegBytes(image: BufferedImage): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Legacy method for uploading images to ImgBB service.
     *
     * **Deprecated**: This method is kept for backwards compatibility but is no longer
     * used by default. The application now uses Appwrite storage via [uploadImage].
     *
     * Converts a [BufferedImage] to base64-encoded JPEG and uploads to ImgBB's API.
     * Requires IMGBB_API_KEY environment variable.
     *
     * @param imageData The BufferedImage to upload
     * @return JSONObject containing ImgBB API response with URLs
     * @throws IllegalStateException if IMGBB_API_KEY environment variable is not set
     * @throws IOException if upload fails
     */
    fun uploadImageToImgBB(imageData: BufferedImage): JSONObject {
        // kept for backwards compatibility; not used by default anymore
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(imageData, "jpg", outputStream)
        val base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray())


        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", base64Image)
            .build()

        val apiKey = EnvUtils.getEnvVariable("IMGBB_API_KEY") ?: throw IllegalStateException("IMGBB_API_KEY environment variable is not set")
        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload?key=${apiKey}")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val jsonResponse = response.body?.string() ?: throw IOException("Response body is null")
            return JSONObject(jsonResponse)
        }
    }

    /**
     * Resizes an image while maintaining aspect ratio, constrained by max dimensions.
     *
     * Calculates new dimensions based on whether the image is landscape or portrait,
     * scaling down only if it exceeds the maximum width or height.
     *
     * @param originalImage Source image to resize
     * @param maxWidth Maximum width constraint in pixels
     * @param maxHeight Maximum height constraint in pixels
     * @return New [BufferedImage] with resized dimensions
     */
    private fun resizeImage(originalImage: BufferedImage, maxWidth: Int, maxHeight: Int): BufferedImage {
        var width = originalImage.width
        var height = originalImage.height

        if (width > height) {
            if (width > maxWidth) {
                height = (height * maxWidth / width)
                width = maxWidth
            }
        } else {
            if (height > maxHeight) {
                width = (width * maxHeight / height)
                height = maxHeight
            }
        }

        val resizedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = resizedImage.createGraphics()
        graphics.drawImage(originalImage, 0, 0, width, height, null)
        graphics.dispose()

        return resizedImage
    }

    /**
     * Crops an image to 16:9 aspect ratio (Full HD/widescreen format).
     *
     * ## Cropping algorithm
     * 1. Calculates target 16:9 aspect ratio (1.777...)
     * 2. Compares current aspect ratio to determine crop direction:
     *    - If wider than 16:9: crop width, keep full height
     *    - If taller than 16:9: crop height, keep full width
     * 3. Centers the crop region (removes equal amounts from both sides)
     * 4. Uses [BufferedImage.getSubimage] to extract the crop region
     * 5. Creates a copy to avoid shared raster issues
     *
     * ## Edge case handling
     * - Uses [coerceIn] to ensure crop dimensions stay within valid bounds
     * - Uses [coerceAtLeast] to ensure crop position is non-negative
     *
     * @param image Source image to crop
     * @return New [BufferedImage] with 16:9 aspect ratio, centered crop
     */
    private fun cropToFullHd(image: BufferedImage): BufferedImage {
        val width = image.width
        val height = image.height
        val aspectRatio = 16.0 / 9.0

        val (rawCropWidth, rawCropHeight) = if (width.toDouble() / height > aspectRatio) {
            Pair((height * aspectRatio).toInt(), height)
        } else {
            Pair(width, (width / aspectRatio).toInt())
        }

        val cropWidth = rawCropWidth.coerceIn(1, width)
        val cropHeight = rawCropHeight.coerceIn(1, height)

        val x = ((width - cropWidth) / 2).coerceAtLeast(0)
        val y = ((height - cropHeight) / 2).coerceAtLeast(0)

        val sub = image.getSubimage(x, y, cropWidth, cropHeight)
        val copy = BufferedImage(sub.width, sub.height, BufferedImage.TYPE_INT_RGB)
        val g = copy.createGraphics()
        g.drawImage(sub, 0, 0, null)
        g.dispose()
        return copy
    }
}