package de.nogaemer.springhomepage.main.images

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.ArrayList

data class Image(
    val thumbnail: String,
    val srcSetArray: ArrayList<String>?,
    var srcSetString: String? = null,
    val deleteUrls: Array<String>? = null
) {
    fun addToSrcSet(src: String) {
        srcSetArray?.add(src);
        srcSetString = srcSetArray?.joinToString(", ")
    }

    fun removeFromSrcSet(src: String) {
        srcSetArray?.remove(src);
        srcSetString = srcSetArray?.joinToString(", ")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Image

        if (thumbnail != other.thumbnail) return false
        if (srcSetArray != other.srcSetArray) return false
        if (srcSetString != other.srcSetString) return false
        if (!deleteUrls.contentEquals(other.deleteUrls)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = thumbnail.hashCode()
        result = 31 * result + (srcSetArray?.hashCode() ?: 0)
        result = 31 * result + (srcSetString?.hashCode() ?: 0)
        result = 31 * result + deleteUrls.contentHashCode()
        return result
    }

    fun delete(image: Image) {
        image.deleteUrls?.forEach {
            val client = OkHttpClient()

            val request = Request.Builder()
                .url(it)
                .delete()
                .build()

            client.newCall(request).execute()
        }
    }
}