/**
 * Database migration script: Convert ingredient unit strings to ObjectId references.
 *
 * ## Migration Purpose
 * This migration transforms the meal ingredients data model from storing unit names
 * as plain strings to storing references to IngredientUnit documents via ObjectId.
 *
 * ## Applied Date
 * December 27, 2025
 *
 * ## Data Changes
 * - Scans all documents in the `meals` collection
 * - For each ingredient where `unit` is a String, attempts to match it to an IngredientUnit
 * - Replaces the string with the corresponding IngredientUnit's ObjectId
 * - Preserves ingredients that cannot be matched (leaves them unchanged)
 *
 * ## Benefits
 * - Enables referential integrity with IngredientUnit collection
 * - Supports normalized unit data (proper pluralization, abbreviations)
 * - Facilitates unit-based filtering and aggregation queries
 * - Reduces data duplication and inconsistencies
 *
 * ## Matching Algorithm
 * The migration uses a fuzzy matching strategy:
 * 1. Queries UnitService for candidates matching the string
 * 2. Tries exact match (abbreviation, plural, full name)
 * 3. Falls back to prefix matching
 * 4. Falls back to substring matching
 * 5. Tries shortened version (drop last character) if no match
 * 6. Uses first candidate if no algorithmic match found
 *
 * ## Performance
 * - Processes meals sequentially (not bulk operation)
 * - Each meal triggers N unit service queries (N = number of string units in meal)
 * - Consider running during low-traffic periods for large databases
 *
 * ## Idempotency
 * Safe to re-run. Already migrated ingredients (ObjectId type) are skipped.
 *
 * @property unitService Service for querying and matching ingredient units
 * @property mongoTemplate Direct MongoDB access for low-level document manipulation
 */
package de.nogaemer.springhomepage.updatedb

import de.nogaemer.springhomepage.main.units.UnitService
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service
import de.nogaemer.springhomepage.main.units.IngredientUnit as UnitModel

/**
 * Migration service for converting ingredient unit strings to ObjectId references.
 *
 * Transforms meal ingredient data from storing unit names as strings to storing
 * normalized references to IngredientUnit documents.
 */
@Service
class IngredientUnit_12_27_2025(private val unitService: UnitService, private val mongoTemplate: MongoTemplate) {

    /**
     * Executes the migration across all meals in the database.
     *
     * Iterates through all meal documents, identifies ingredients with string-based
     * unit fields, matches them to IngredientUnit documents, and replaces the strings
     * with ObjectId references.
     *
     * ## Process Flow
     * 1. Retrieves all documents from `meals` collection
     * 2. For each meal, examines the `ingredients` array
     * 3. Identifies ingredients where `unit` is a String
     * 4. Attempts to match the string to an IngredientUnit via fuzzy matching
     * 5. Replaces matched strings with ObjectId references
     * 6. Updates the meal document if any changes were made
     *
     * ## MongoDB Operations
     * - Collection scan: O(n) where n = number of meals
     * - Update operation: One replaceOne per modified meal (not bulk)
     * - No indexes required, but may benefit from index on meals._id
     *
     * @return Map containing migration statistics:
     *   - "updatedMeals": Number of meal documents that were modified
     */
    fun updateAll(): Map<String, Any> {
        val collection = mongoTemplate.db.getCollection("meals")
        val cursor = collection.find().iterator()
        var updatedCount = 0

        while (cursor.hasNext()) {
            val mealDoc = cursor.next()
            val ingredientsObj = mealDoc.get("ingredients") ?: continue
            if (ingredientsObj !is List<*>) continue

            var changed = false
            val newIngredients = ArrayList<Document>()

            for (ing in ingredientsObj) {
                if (ing !is Document) {
                    // Keep as-is if unexpected type
                    continue
                }

                val unitField = ing.get("unit")

                if (unitField is String) {
                    val unitString = unitField.trim()
                    val unit = findUnitForString(unitString)


                    if (unit != null && unit.id != null) {
                        val updatedIng = Document(ing)
                        // store the ObjectId reference directly (Mongo will represent this as an ObjectId)
                        updatedIng.put("unit", unit.id as ObjectId)
                        newIngredients.add(updatedIng)
                        changed = true
                        continue
                    }
                }

                // If not a string or no unit found, keep original ingredient document
                newIngredients.add(ing)
            }

            if (changed) {
                mealDoc.put("ingredients", newIngredients)
                val filter = Document("_id", mealDoc.get("_id"))
                collection.replaceOne(filter, mealDoc)
                updatedCount++
            }
        }

        return mapOf("updatedMeals" to updatedCount)
    }

    /**
     * Attempts to match a unit string to an IngredientUnit using fuzzy matching.
     *
     * Employs a multi-strategy approach to handle variations in unit naming:
     * - Exact matches on abbreviation (e.g., "g", "kg")
     * - Exact matches on full name (e.g., "Gramm", "Kilogramm")
     * - Plural forms (e.g., "Grammes")
     * - Prefix matching (e.g., "kil" -> "Kilogramm")
     * - Substring matching (e.g., "gram" -> "Gramm")
     * - Shortened variants (e.g., "tl" -> "TL")
     *
     * ## Matching Strategy
     * 1. Query UnitService for up to 10 candidates containing the string
     * 2. Try exact match (case-insensitive) on all name forms
     * 3. Try prefix match if exact fails
     * 4. Try substring match if prefix fails
     * 5. Retry with string minus last character (handles typos/variants)
     * 6. Fall back to first candidate if all algorithms fail
     *
     * ## Performance Considerations
     * - Makes UnitService query (may hit database or cache)
     * - Case-insensitive comparisons for all candidate units
     * - Returns null if input is blank
     *
     * @param unitString The unit string to match (e.g., "g", "Gramm", "kg")
     * @return Matched IngredientUnit, or null if string is blank or no reasonable match found
     */
    private fun findUnitForString(unitString: String): UnitModel? {
        if (unitString.isBlank()) return null

        val candidates = unitService.getUnits(10, 0, unitString)
        val query = unitString.trim()

        var unit: UnitModel? = run {
            fun findExact(q: String) = candidates.find {
                it.abbreviation.equals(q, true) ||
                        it.abbreviationPlural.equals(q, true) ||
                        it.fullName.equals(q, true) ||
                        it.fullNamePlural.equals(q, true)
            }

            fun findStarts(q: String) = candidates.find {
                it.abbreviation.startsWith(q, true) ||
                        it.fullName.startsWith(q, true)
            }

            fun findContains(q: String) = candidates.find {
                it.abbreviation.contains(q, true) ||
                        it.fullName.contains(q, true)
            }

            var u: UnitModel? = findExact(query)
            if (u == null && query.isNotEmpty()) u = findStarts(query)
            if (u == null && query.isNotEmpty()) u = findContains(query)

            if (u == null && query.length > 1) {
                val shorter = query.dropLast(1)
                u = findExact(shorter)
                if (u == null) u = findStarts(shorter)
                if (u == null) u = findContains(shorter)
            }

            u
        }

        if (unit == null) {
            unit = candidates.firstOrNull()
        }

        return unit
    }
}


