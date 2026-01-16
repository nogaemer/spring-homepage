package de.nogaemer.springhomepage.main.images

/**
 * Represents a processed image with responsive preview URLs and cleanup metadata.
 *
 * This data class encapsulates all information needed to display an image responsively
 * and manage its lifecycle in cloud storage.
 *
 * ## Responsive image delivery
 * The [srcSetArray] and [srcSetString] properties enable responsive images using the HTML
 * srcset attribute, allowing browsers to select appropriate resolution based on device
 * and viewport size.
 *
 * ## Storage cleanup
 * The [deleteUrls] property stores Appwrite file IDs (not full URLs despite the name)
 * that should be deleted when this image is removed. This enables proper cleanup of
 * cloud storage resources.
 *
 * @property thumbnail URL for small preview image (typically 200px width) for gallery views
 * @property srcSetArray List of URLs at different resolutions for responsive image selection
 * @property srcSetString Formatted srcset string (e.g., "url1 360w, url2 640w") for HTML use
 * @property deleteUrls Array of Appwrite file IDs to delete when cleaning up this image
 */
data class Image(
    val thumbnail: String,
    val srcSetArray: ArrayList<String>?,
    var srcSetString: String? = null,
    val deleteUrls: Array<String>? = null
) {
    /**
     * Adds a new URL to the srcSet array and updates the srcSet string.
     *
     * Useful for dynamically adding additional resolutions after initial creation.
     *
     * @param src URL to add to the srcSet
     */
    fun addToSrcSet(src: String) {
        srcSetArray?.add(src)
        srcSetString = srcSetArray?.joinToString(", ")
    }

    /**
     * Removes a URL from the srcSet array and updates the srcSet string.
     *
     * Useful for removing invalid or expired URLs from the responsive image set.
     *
     * @param src URL to remove from the srcSet
     */
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