package de.nogaemer.springhomepage.meals.models

data class ImgLink(
    val thumbnail: String,
    val srcSetArray: ArrayList<String>?,
    var srcSetString: String? = null,
    val deleteUrl: String? = null
) {
    fun addToSrcSet(src: String) {
        srcSetArray?.add(src);
        srcSetString = srcSetArray?.joinToString(", ")
    }

    fun removeFromSrcSet(src: String) {
        srcSetArray?.remove(src);
        srcSetString = srcSetArray?.joinToString(", ")
    }
}