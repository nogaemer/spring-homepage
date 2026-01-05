package de.nogaemer.springhomepage.updatedb

import de.nogaemer.springhomepage.main.units.UnitService
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service
import de.nogaemer.springhomepage.main.units.IngredientUnit as UnitModel

@Service
class IngredientUnit_12_27_2025(private val unitService: UnitService, private val mongoTemplate: MongoTemplate) {

    /**
     * Scans the `meals` collection for ingredients where `unit` is stored as a string
     * and replaces it with the referenced IngredientUnit's ObjectId.
     * Returns a small report map with the number of meals updated.
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

