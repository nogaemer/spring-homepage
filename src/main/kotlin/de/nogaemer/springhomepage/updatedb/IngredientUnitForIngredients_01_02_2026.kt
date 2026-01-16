/**
 * Database migration script: Convert ingredient name strings to ObjectId references.
 *
 * ## Migration Purpose
 * This migration transforms meal ingredient data from storing ingredient names as
 * plain strings to storing references to Ingredient documents via ObjectId. This
 * establishes proper referential integrity and enables normalized ingredient data.
 *
 * ## Applied Date
 * January 2, 2026
 *
 * ## Data Changes
 * - Scans all documents in the `meals` collection
 * - For each ingredient where `name` is a String, attempts to match it to an Ingredient
 * - Adds an `ingredient` field containing the Ingredient's ObjectId
 * - Preserves the `name` field for compatibility (dual storage during transition)
 * - Skips ingredients that already have an `ingredient` ObjectId field
 *
 * ## Benefits
 * - Enables referential integrity with Ingredient collection
 * - Supports normalized ingredient data (spelling, categorization)
 * - Facilitates ingredient-based filtering and search
 * - Reduces data inconsistencies from typos and variations
 *
 * ## Matching Strategy
 * 1. Manual Mapping - Hardcoded mappings for known problematic ingredient names
 * 2. Text Normalization - Removes diacritics, handles parentheses, splits on separators
 * 3. Fuzzy Search - Uses IngredientService queries with exact/prefix/substring matching
 * 4. Strict Fallback - Does NOT blindly accept candidates to avoid incorrect mappings
 *
 * ## Performance
 * - Processes meals sequentially (not bulk operation)
 * - Each meal may trigger multiple IngredientService queries
 * - Text normalization overhead per ingredient
 * - Consider running during low-traffic periods for large databases
 *
 * ## Known Issues
 * - Some ingredients may not match due to naming variations
 * - Manual mapping required for edge cases (compound ingredients, brand names)
 * - May need multiple runs with updated manual mappings
 *
 * ## Idempotency
 * Safe to re-run. Ingredients with existing `ingredient` ObjectId fields are skipped.
 *
 * @property ingredientService Service for querying and matching ingredients
 * @property mongoTemplate Direct MongoDB access for low-level document manipulation
 */
package de.nogaemer.springhomepage.updatedb

import de.nogaemer.springhomepage.main.ingredients.Ingredient
import de.nogaemer.springhomepage.main.ingredients.IngredientService
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service
import java.text.Normalizer

/**
 * Migration service for converting ingredient name strings to ObjectId references.
 *
 * Transforms meal ingredient data from storing ingredient names as strings to storing
 * normalized references to Ingredient documents. Uses a combination of manual mappings
 * and fuzzy text matching to handle naming variations.
 */
@Service
class IngredientUnitForIngredients_01_02_2026(
    private val ingredientService: IngredientService,
    private val mongoTemplate: MongoTemplate
) {

    /**
     * Manual ingredient name to ObjectId mappings for problematic cases.
     *
     * These hardcoded mappings handle:
     * - Compound ingredients (e.g., "Paprikaschote(n), rote")
     * - Brand names (e.g., "HENGLEIN Frischer Hefeteig")
     * - Descriptive variants (e.g., "Kartoffeln, mehligkochende")
     * - Ambiguous terms that fuzzy matching would resolve incorrectly
     *
     * IDs correspond to documents in the `ingredients` collection from the provided
     * meal-api-db.ingredients.json export.
     */
    private val manualMapping = mapOf(
        "Paprikaschote(n), rote" to "694c0f30ab870b7711e9d9b9", // Paprika
        "Paprikaschote(n), gelbe" to "694c0f30ab870b7711e9d9b9", // Paprika
        "Gemüsezwiebel(n)" to "694c0f30ab870b7711e9d90d", // Zwiebeln
        "Strauchtomate(n) , reife" to "694c0f30ab870b7711e9d91a", // Tomate
        "Rigatoni" to "694c0f30ab870b7711e9d8d5", // Nudeln (Generic fallback)
        "Kochschinken" to "694c0f30ab870b7711e9d8e8", // Schinken
        "Backkakao" to "694c0f30ab870b7711e9da65", // Kakao
        "Öl zum Backen" to "694c0f30ab870b7711e9d9a4", // Öl
        "Fett für die Form" to "694c0f30ab870b7711e9d980", // Butter/Fett
        "Snackgurke(n)" to "694c0f30ab870b7711e9d918", // Gurke
        "Romatomate(n)" to "694c0f30ab870b7711e9d91a", // Tomate
        "HENGLEIN Frischer Hefeteig" to "694c0f30ab870b7711e9da68", // Hefe
        "Currypulver , mildes" to "694c0f30ab870b7711e9da26", // Curry
        "Koriandergrün" to "694c0f30ab870b7711e9d9c1", // Koriander
        "Zimtpulver" to "694c0f30ab870b7711e9d9c5", // Zimt
        "Mandellikör" to "694c0f30ab870b7711e9da5b", // Likör/Amaretto
        "Fischfilet(s) (Pangasiusfilet), küchenfertig" to "6959390f2e5b0c7db185a724", // Fischfilet
        "Bio-Orange(n)" to "694c0f30ab870b7711e9d954", // Orange
        "Orangenöl" to "694c0f30ab870b7711e9d954", // Orange
        "Kartoffeln, mehligkochende" to "6959390f2e5b0c7db185a725", // Kartoffel
        "Frühstücksspeck , gewürfelt" to "694c0f30ab870b7711e9d8e7", // Speck
        "Cheddarkäse , gerieben" to "694c0f30ab870b7711e9d982", // Cheddar
        "Eier" to "694c0f30ab870b7711e9d99b", // Eier
        "Knoblauchzehe(n)" to "694c0f30ab870b7711e9d90f", // Knoblauch
        "Knoblauchzehe(n) , gehackt" to "694c0f30ab870b7711e9d90f", // Knoblauch
        "Dose/n Safranfäden" to "6959390f2e5b0c7db185a72a", // Safran
        "Lebensmittelfarbe , rot" to "6959390f2e5b0c7db185a726", // Lebensmittelfarbe Rot
        "Chilischote(n) , rote" to "694c0f30ab870b7711e9d91f", // Chili/Paprika
        "Hoisinsauce" to "695937272e5b0c7db185a714", // Hoisinsauce
        "Kräuter, gemischte , TK" to "69593c3f2e5b0c7db185a736", // Kräuter gemischt (Not tea!)
        "Black Tiger Garnelen" to "694c0f30ab870b7711e9d8fa", // Garnelen
        "Edamer Käse , geriebener" to "694c0f30ab870b7711e9d988", // Edamer
        "Paprikaschote(n)" to "694c0f30ab870b7711e9d9b9", // Paprika (Generic)
        "Gemüsebrühe oder Gemüsefond" to "694c0f30ab870b7711e9d9f1", // Gemüsebrühe
        "Sahnejoghurt , griechischer" to "69593ca22e5b0c7db185a73a" // Griechischer Joghurt
    )

    /**
     * Executes the migration across all meals in the database.
     *
     * Iterates through all meal documents, identifies ingredients with string-based
     * name fields, matches them to Ingredient documents, and adds ObjectId references.
     *
     * ## Process Flow
     * 1. Retrieves all documents from `meals` collection
     * 2. For each meal, examines the `ingredients` array
     * 3. Skips ingredients that already have an `ingredient` ObjectId field
     * 4. For ingredients with string `name` fields:
     *    - Checks manual mapping first
     *    - Falls back to fuzzy text matching via IngredientService
     * 5. Adds `ingredient` ObjectId field to matched ingredients
     * 6. Updates the meal document if any changes were made
     *
     * ## MongoDB Operations
     * - Collection scan: O(n) where n = number of meals
     * - Update operation: One replaceOne per modified meal (not bulk)
     * - No indexes required, but may benefit from index on meals._id
     *
     * ## Data Preservation
     * - Original `name` field is preserved (not removed)
     * - Allows gradual transition and rollback if needed
     * - Enables dual-field queries during migration period
     *
     * @return Map containing migration statistics:
     *   - "updatedMeals": Number of meal documents that were modified
     */
    fun updateAll(): Map<String, Int> {
        val collection = mongoTemplate.db.getCollection("meals")
        val cursor = collection.find().iterator()
        var updatedCount = 0

        while (cursor.hasNext()) {
            val mealDoc = cursor.next()
            val ingredientsObj = mealDoc.get("ingredients") ?: continue
            if (ingredientsObj !is List<*>) continue

            var changed = false
            val newIngredients = ArrayList<Any>()

            for (ing in ingredientsObj) {
                if (ing !is Document) {
                    newIngredients.add(ing as Any)
                    continue
                }

                // Check if already migrated (has "ingredient" as ObjectId)
                val existingIngRef = ing.get("ingredient")
                if (existingIngRef is ObjectId) {
                    newIngredients.add(ing)
                    continue
                }

                val ingredientName = ing.get("name")
                if (ingredientName is String) {
                    val ingredientString = ingredientName.trim()

                    // Try to find the ingredient
                    val ingredient = findIngredientForString(ingredientString)

                    if (ingredient != null && ingredient.id != null) {
                        val updatedIng = Document(ing)
                        // store the ObjectId reference directly
                        updatedIng.put("ingredient", ingredient.id as ObjectId)
                        newIngredients.add(updatedIng)
                        changed = true
                        continue
                    }
                }

                // If no match found, keep original
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
     * Attempts to match an ingredient name string to an Ingredient using fuzzy matching.
     *
     * Employs a sophisticated multi-strategy approach to handle the complexity of
     * ingredient naming variations, including:
     * - Compound names (e.g., "Paprikaschote(n), rote")
     * - Parenthetical annotations (e.g., "Knoblauchzehe(n)")
     * - Multiple separators (/, ,, ;, "oder", etc.)
     * - Unit suffixes embedded in names (e.g., "Knoblauchzehe")
     * - Unicode normalization (diacritics, special characters)
     *
     * ## Matching Strategy
     * 1. **Manual Mapping Check**: Consults hardcoded mapping table first
     * 2. **Text Normalization**: Removes diacritics and normalizes case
     * 3. **Text Splitting**: Splits on separators like "/", ",", "oder"
     * 4. **Unit Suffix Stripping**: Removes common unit suffixes (zehe, stück, etc.)
     * 5. **Candidate Search**: Queries IngredientService for each processed variant
     * 6. **Exact Match**: Tries exact match on normalized ingredient names
     * 7. **Prefix Match**: Falls back to prefix matching
     * 8. **Substring Match**: Falls back to substring matching
     * 9. **Strict Fallback**: Returns null rather than accepting poor matches
     *
     * ## Text Normalization Details
     * - Removes Unicode combining diacritical marks (ä -> a, ö -> o)
     * - Converts to lowercase for case-insensitive comparison
     * - Removes parenthetical content for cleaner matching
     * - Splits on separators: /, ,, ;, (, ), +, -, "oder", "or", "bzw"
     *
     * ## Unit Suffix Handling
     * Common German unit suffixes are stripped to extract base ingredient:
     * - zehe/zehen (clove/cloves)
     * - stück/stücke (piece/pieces)
     * - scheibe (slice)
     * - bund (bunch)
     * - tl/el (teaspoon/tablespoon)
     *
     * ## Performance Considerations
     * - Makes multiple IngredientService queries (may hit database or cache)
     * - Text normalization overhead for each candidate
     * - Regex operations for splitting and normalization
     * - May query up to 50 candidates per search term
     *
     * ## Safety
     * - Returns null for ambiguous matches rather than guessing
     * - Avoids "Weizenmehl" errors (generic fallbacks)
     * - Requires reasonable confidence in match quality
     *
     * @param ingredientString The ingredient name to match (e.g., "Paprikaschote(n), rote")
     * @return Matched Ingredient, or null if string is blank or no confident match found
     */
    private fun findIngredientForString(ingredientString: String): Ingredient? {
        if (ingredientString.isBlank()) return null

        // 1. Check Manual Mapping First
        val mappedId = manualMapping[ingredientString]
        if (mappedId != null) {
            val ing = ingredientService.findById(mappedId)
            if (ing != null) return ing
        }

        /**
         * Normalizes a string by removing diacritics and converting to lowercase.
         *
         * Uses Unicode normalization (NFD) to decompose accented characters into
         * base character + combining diacritical mark, then removes the marks.
         *
         * Examples:
         * - "Paprikaschote" -> "paprikaschote"
         * - "Käse" -> "kase"
         * - "Grüne Bohnen" -> "grune bohnen"
         */
        fun normalize(s: String) = Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

        val cleaned = ingredientString.trim()
        val cleanedNoParens = cleaned.replace(Regex("\\(.*?\\)"), "").trim()

        // Split on common separators (/, ,, ;, parentheses, +, -, "oder", "or", "bzw")
        val splitRegex = Regex("\\s*[/,;()+-]\\s*|\\s+(?:oder|or|bzw|bzw\\.|alternativ)\\s+", RegexOption.IGNORE_CASE)
        val parts = cleanedNoParens.split(splitRegex).map { it.trim() }.filter { it.isNotEmpty() }

        // Common German unit suffixes that may be part of ingredient names
        val unitSuffixes =
            listOf("zehe", "zehen", "stück", "stueck", "stücke", "kopf", "knolle", "scheibe", "bund", "tl", "el")
        val unitSuffixesNormalized = unitSuffixes.map { normalize(it) }

        val allCandidates = ArrayList<Ingredient>()

        // 2. Search logic: Query IngredientService for each split part and stripped variants
        if (parts.isEmpty()) {
            val safeQuery = Regex.escape(cleaned)
            allCandidates.addAll(ingredientService.getIngredients(50, 0, safeQuery))
        } else {
            for (part in parts) {
                if (part.isBlank()) continue

                val searchTerms = LinkedHashSet<String>()
                searchTerms.add(part)

                val pnorm = normalize(part)

                // Generate search term variants by stripping unit suffixes
                for (suf in unitSuffixesNormalized) {
                    if (pnorm.endsWith(suf)) {
                        val stripped = pnorm.removeSuffix(suf).trim()
                        if (stripped.isNotEmpty()) {
                            searchTerms.add(stripped)
                            searchTerms.add(stripped.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                        }
                    }
                }

                // Query IngredientService for all search term variants
                for (term in searchTerms) {
                    val safe = Regex.escape(term)
                    val list = ingredientService.getIngredients(50, 0, safe)
                    for (c in list) if (!allCandidates.any { it.id == c.id }) allCandidates.add(c)
                }
            }
        }

        // 3. Match Logic: Try exact, prefix, and substring matching on candidates
        if (allCandidates.isNotEmpty()) {
            /**
             * Attempts to match ingredient candidates using exact, prefix, or substring matching.
             *
             * @param cands List of candidate ingredients to match against
             * @param qnorm Normalized query string to match
             * @return First matching ingredient, or null if no match found
             */
            fun matchCandidates(cands: List<Ingredient>, qnorm: String): Ingredient? {
                if (qnorm.isEmpty()) return null
                var match = cands.find { normalize(it.name) == qnorm }
                if (match == null) match = cands.find { normalize(it.name).startsWith(qnorm) }
                if (match == null) match = cands.find { normalize(it.name).contains(qnorm) }
                return match
            }

            // Try matching each split part against all candidates
            for (part in parts) {
                val pnorm = normalize(part)
                val found = matchCandidates(allCandidates, pnorm)
                if (found != null) return found
            }

            // Try matching the full cleaned string
            val queryNorm = normalize(cleaned)
            val foundFull = matchCandidates(allCandidates, queryNorm)
            if (foundFull != null) return foundFull

            // STRICTER FALLBACK: Do NOT return random candidates to avoid incorrect mappings.
            // Only return if we have a relatively safe match or if the candidate list is
            // very small and precise. For now, return null to prevent errors like mapping
            // "Weizenmehl" incorrectly.
            return null
        }

        return null
    }
}
