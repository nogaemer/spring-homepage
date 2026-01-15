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

@Service
class ImageService(
    private val appwriteService: AppwriteService
) {
    fun uploadImage(base64Input: String): Image {
        val imageBytes = Base64.getDecoder().decode(base64Input)

        val inputStream = ByteArrayInputStream(imageBytes)
        val originalImage = ImageIO.read(inputStream) ?: throw IOException("Failed to read image from input stream")
        inputStream.close()

        val croppedImage = cropToFullHd(originalImage)

        // convert images to bytes
        val byteImage = bufferedImageToJpegBytes(croppedImage)

        // Upload to Appwrite
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

    private fun bufferedImageToJpegBytes(image: BufferedImage): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", outputStream)
        return outputStream.toByteArray()
    }

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