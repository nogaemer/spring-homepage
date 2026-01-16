/**
 * Database migration script: Consolidate meal image links into unified structure.
 *
 * ## Migration Purpose
 * This migration was designed to transform the meal image data model from storing
 * separate `imageSrc` and `imageSrcSet` arrays to a unified `images` array containing
 * structured ImgLink objects.
 *
 * ## Applied Date
 * November 16, 2024
 *
 * ## Data Changes (CURRENTLY DISABLED)
 * The migration logic is commented out but was intended to:
 * - Scan meals with `imageSrc` (String array) and `imageSrcSet` (String array) fields
 * - Combine related image URLs from both arrays
 * - Create ImgLink objects with primary URL, URL set, and metadata
 * - Store the unified structure in a new `images` field
 * - Remove the legacy `imageSrc` and `imageSrcSet` fields
 *
 * ## Implementation Status
 * **This migration is currently disabled.** The `updateAll()` method contains commented-out
 * code, so calling this migration has no effect. This suggests:
 * - The migration was either already successfully applied, or
 * - The data model change was implemented differently, or
 * - The migration is being preserved for reference/rollback purposes
 *
 * ## Original Algorithm
 * The commented code shows the intended approach:
 * 1. Query meals where `imageSrc` exists and is a String
 * 2. Remove duplicate URLs from both arrays
 * 3. Combine matching indices from `imageSrc` and `imageSrcSet`
 * 4. Create ImgLink objects with main URL and all variants
 * 5. Update meal with new `images` array and remove old fields
 *
 * @property mongoTemplate Direct MongoDB access for low-level document operations
 */
package de.nogaemer.springhomepage.updatedb

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service

/**
 * Migration service for consolidating meal image link structures.
 *
 * Contains disabled migration logic for transforming legacy image storage format
 * to a unified ImgLink-based structure.
 */
@Service
class LinkInMeal_11_16_2024(private val mongoTemplate: MongoTemplate) {

    /**
     * Executes the image link consolidation migration.
     *
     * **This method is currently a no-op.** All migration logic is commented out,
     * so calling this method has no effect on the database. The commented code
     * is preserved for reference.
     *
     * If this migration needs to be re-enabled, uncomment the code and ensure:
     * - The ImgLink model class exists and is properly imported
     * - The target data structure matches application expectations
     * - A test run is performed on non-production data first
     *
     * @return Unit (void) - no data is modified or returned
     */
    fun updateAll() {
//        val criteria = Criteria().andOperator(
//            Criteria.where("imageSrc").exists(true),
//            Criteria.where("imageSrc").type(JsonSchemaObject.Type.stringType())
//        )
//        val query = Query(criteria)
//
//
//        mongoTemplate.find(query, Meal::class.java).forEach { meal ->
//            val oldLink = removeClones(meal.imageSrc as ArrayList<String>)
//            val oldLinkSet = removeClones(meal.imageSrcSet as ArrayList<String>)
//
//            val newLinkSet = ArrayList<ImgLink>()
//
//            combineLinks(oldLink, oldLinkSet).forEach {
//                val newLink = ImgLink(
//                    it[0],
//                    it,
//                    null)
//
//                newLinkSet.add(newLink)
//            }
//
//            val update = Update()
//                .set("images", newLinkSet)
//                .unset("imageSrcSet")
//                .unset("imageSrc")
//
//            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").`is`(meal.id)), update, Meal::class.java)
//        }
    }

    /**
     * Combines image URLs from separate imageSrc and imageSrcSet arrays.
     *
     * Merges corresponding indices from both arrays, creating a unified structure
     * where each primary image URL is associated with its responsive image variants.
     *
     * ## Algorithm
     * 1. Creates a result entry for each imageSrc URL
     * 2. For each corresponding imageSrcSet entry, splits on comma separators
     * 3. Adds unique URLs from imageSrcSet to the corresponding result entry
     * 4. Result: ArrayList of ArrayList<String> where each inner list is [mainUrl, variant1, variant2, ...]
     *
     * ## Example
     * ```
     * imageSrc = ["image1.jpg", "image2.jpg"]
     * imageSrcSet = ["image1-small.jpg,image1-large.jpg", "image2-small.jpg"]
     * Result = [["image1.jpg", "image1-small.jpg", "image1-large.jpg"],
     *           ["image2.jpg", "image2-small.jpg"]]
     * ```
     *
     * @param imageSrc Array of primary image URLs (one per image)
     * @param imageSrcSet Array of comma-separated responsive image variants (parallel to imageSrc)
     * @return ArrayList of ArrayList<String> where each inner list contains all URLs for one logical image
     */
    private fun combineLinks(imageSrc: ArrayList<String>, imageSrcSet: ArrayList<String>): ArrayList<ArrayList<String>> {
        val result = ArrayList<ArrayList<String>>()

        imageSrc.forEach {
            result.add(ArrayList(listOf(it)))
        }

        for ((index, s) in imageSrcSet.withIndex()) {
            val it = imageSrcSet[index]

            it.split(",").forEach { src ->
                if (!result[index].contains(src)) {
                    result[index].add(src)
                }
            }
        }

        return result;
    }

    /**
     * Removes duplicate URLs from an image source array.
     *
     * Deduplicates image URLs by trimming whitespace and using a HashSet to track
     * unique values. This ensures the final image structure contains no redundant URLs.
     *
     * ## Process
     * 1. Removes all whitespace from each URL string (handles srcset format variations)
     * 2. Converts to Set to eliminate duplicates
     * 3. Returns ArrayList of unique URLs
     *
     * ## Example
     * ```
     * Input:  ["image1.jpg", "image1.jpg ", " image2.jpg", "image1.jpg"]
     * Output: ["image1.jpg", "image2.jpg"]
     * ```
     *
     * @param imageSrcSet ArrayList of image URLs that may contain duplicates or whitespace
     * @return ArrayList<String> containing only unique, whitespace-trimmed URLs
     */
    private fun removeClones(imageSrcSet: ArrayList<String>): ArrayList<String> {
        val set = HashSet<String>()
        val result = ArrayList<String>()

        val trimmedImageSrcSet = imageSrcSet.map { it.replace("\\s".toRegex(), "") }.toSet()
        trimmedImageSrcSet.forEach { s -> set.add(s) }

        result.addAll(set)
        return result
    }


}
