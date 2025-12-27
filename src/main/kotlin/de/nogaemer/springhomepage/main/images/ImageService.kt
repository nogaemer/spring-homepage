package de.nogaemer.springhomepage.main.images

import de.nogaemer.springhomepage.utils.EnvUtils
import io.github.cdimascio.dotenv.Dotenv
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
class ImageService {
    fun uploadImage(base64Input: String): Image {
        val imageBytes = Base64.getDecoder().decode(base64Input)

        val inputStream = ByteArrayInputStream(imageBytes)
        val originalImage = ImageIO.read(inputStream) ?: throw IOException("Failed to read image from input stream")
        inputStream.close()

        val croppedImage = cropTo3by2Ratio(originalImage)
        val resizedImage800 = resizeImage(croppedImage, 800, 534)
        val resizedImage360 = resizeImage(croppedImage, 360, 240)


        val image360 = uploadImageToImgBB(resizedImage360).getJSONObject("data")
        val image800 = uploadImageToImgBB(resizedImage800).getJSONObject("data")

        val image360Url = image360.getJSONObject("image").getString("url")
        val image640Url = image800.getJSONObject("medium").getString("url") ?: image800.getJSONObject("image").getString("url")
        val image800Url = image800.getJSONObject("image").getString("url")

        return Image(
            thumbnail = image800.getJSONObject("thumb").getString("url"),
            srcSetArray = arrayListOf(
                image360Url,
                image640Url,
                image800Url
            ),
            srcSetString = image360Url + " 360w, " +
                    image640Url + " 640w, " +
                    image800Url + " 800w",
            deleteUrls = arrayOf(
                image360.getString("delete_url"),
                image800.getString("delete_url")
            ),
        )
    }

    fun uploadImageToImgBB(imageData: BufferedImage): JSONObject {
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

    private fun cropTo3by2Ratio(image: BufferedImage): BufferedImage {
        val width = image.width
        val height = image.height
        val aspectRatio = 3.0 / 2.0

        val (cropWidth, cropHeight) = if (width.toDouble() / height > aspectRatio) {
            // Image is wider than 3:2, crop width
            Pair((height * aspectRatio).toInt(), height)
        } else {
            // Image is taller than 3:2, crop height
            Pair(width, (width / aspectRatio).toInt())
        }

        val x = (width - cropWidth) / 2
        val y = (height - cropHeight) / 2

        return image.getSubimage(x, y, cropWidth, cropHeight)
    }
}