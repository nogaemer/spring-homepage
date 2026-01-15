package de.nogaemer.springhomepage.main.images

data class Image(
    val thumbnail: String,
    val srcSetArray: ArrayList<String>?,
    var srcSetString: String? = null,
    // store file ids for deletion (handled by AppwriteService or controller) instead of raw delete URLs
    val deleteUrls: Array<String>? = null
) {
    fun addToSrcSet(src: String) {
        srcSetArray?.add(src)
        srcSetString = srcSetArray?.joinToString(", ")
    }

    fun removeFromSrcSet(src: String) {
        srcSetArray?.remove(src)
        srcSetString = srcSetArray?.joinToString(", ")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Image

        if (thumbnail != other.thumbnail) return false
        if (srcSetArray != other.srcSetArray) return false
        if (srcSetString != other.srcSetString) return false
        if (deleteUrls != null) {
            if (other.deleteUrls == null) return false
            if (!deleteUrls.contentEquals(other.deleteUrls)) return false
        } else if (other.deleteUrls != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = thumbnail.hashCode()
        result = 31 * result + (srcSetArray?.hashCode() ?: 0)
        result = 31 * result + (srcSetString?.hashCode() ?: 0)
        result = 31 * result + (deleteUrls?.contentHashCode() ?: 0)
        return result
    }
}