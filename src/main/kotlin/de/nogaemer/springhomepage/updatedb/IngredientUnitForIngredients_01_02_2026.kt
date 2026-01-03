package de.nogaemer.springhomepage.updatedb

import de.nogaemer.springhomepage.main.meals.ingredients.Ingredient
import de.nogaemer.springhomepage.main.meals.ingredients.IngredientService
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service
import java.text.Normalizer

@Service
class IngredientUnitForIngredients_01_02_2026(
    private val ingredientService: IngredientService,
    private val mongoTemplate: MongoTemplate
) {

    // Manual mapping for known problematic ingredients to their correct DB ObjectIds
    // IDs are based on the provided meal-api-db.ingredients.json
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
     * Scans the `meals` collection for ingredients where `ingredient` is stored as a string
     * and replaces it with the referenced IngredientUnit's ObjectId.
     * Returns a small report map with the number of meals updated.
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

    private fun findIngredientForString(ingredientString: String): Ingredient? {
        if (ingredientString.isBlank()) return null

        // 1. Check Manual Mapping First
        val mappedId = manualMapping[ingredientString]
        if (mappedId != null) {
            val ing = ingredientService.findById(mappedId)
            if (ing != null) return ing
        }

        // Normalization helper
        fun normalize(s: String) = Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

        val cleaned = ingredientString.trim()
        val cleanedNoParens = cleaned.replace(Regex("\\(.*?\\)"), "").trim()

        // Split on common separators
        val splitRegex = Regex("\\s*[/,;()+-]\\s*|\\s+(?:oder|or|bzw|bzw\\.|alternativ)\\s+", RegexOption.IGNORE_CASE)
        val parts = cleanedNoParens.split(splitRegex).map { it.trim() }.filter { it.isNotEmpty() }

        val unitSuffixes =
            listOf("zehe", "zehen", "stück", "stueck", "stücke", "kopf", "knolle", "scheibe", "bund", "tl", "el")
        val unitSuffixesNormalized = unitSuffixes.map { normalize(it) }

        val allCandidates = ArrayList<Ingredient>()

        // 2. Search logic
        if (parts.isEmpty()) {
            val safeQuery = Regex.escape(cleaned)
            allCandidates.addAll(ingredientService.getIngredients(50, 0, safeQuery))
        } else {
            for (part in parts) {
                if (part.isBlank()) continue

                val searchTerms = LinkedHashSet<String>()
                searchTerms.add(part)

                val pnorm = normalize(part)

                // Add stripped versions
                for (suf in unitSuffixesNormalized) {
                    if (pnorm.endsWith(suf)) {
                        val stripped = pnorm.removeSuffix(suf).trim()
                        if (stripped.isNotEmpty()) {
                            searchTerms.add(stripped)
                            searchTerms.add(stripped.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                        }
                    }
                }

                // Query service
                for (term in searchTerms) {
                    val safe = Regex.escape(term)
                    val list = ingredientService.getIngredients(50, 0, safe)
                    for (c in list) if (!allCandidates.any { it.id == c.id }) allCandidates.add(c)
                }
            }
        }

        // 3. Match Logic
        if (allCandidates.isNotEmpty()) {
            // Helper to match exact/starts/contains
            fun matchCandidates(cands: List<Ingredient>, qnorm: String): Ingredient? {
                if (qnorm.isEmpty()) return null
                var match = cands.find { normalize(it.name) == qnorm }
                if (match == null) match = cands.find { normalize(it.name).startsWith(qnorm) }
                if (match == null) match = cands.find { normalize(it.name).contains(qnorm) }
                return match
            }

            // Try matching parts
            for (part in parts) {
                val pnorm = normalize(part)
                val found = matchCandidates(allCandidates, pnorm)
                if (found != null) return found
            }

            // Try matching full string
            val queryNorm = normalize(cleaned)
            val foundFull = matchCandidates(allCandidates, queryNorm)
            if (foundFull != null) return foundFull

            // STRICTER FALLBACK: Do NOT return random candidates.
            // Only return if we have a relatively safe match or if the candidate list is very small and precise.
            // For now, return null to avoid "Weizenmehl" errors.
            return null
        }

        return null
    }
}
