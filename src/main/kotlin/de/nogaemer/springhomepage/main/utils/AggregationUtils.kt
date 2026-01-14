package de.nogaemer.springhomepage.main.utils

import org.bson.Document
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation

object AggregationUtils {
    fun lookupField(lookupArray: String, itemIdField: String, targetField: String): Document {
        return Document(
            "\$let", Document()
                .append(
                    "vars", Document(
                        "match", Document(
                            "\$arrayElemAt", listOf(
                                Document(
                                    "\$filter", Document()
                                        .append("input", "\$$lookupArray")
                                        .append("as", "el")
                                        .append("cond", Document("\$eq", listOf("\$\$el._id", "\$\$item.$itemIdField")))
                                ), 0
                            )
                        )
                    )
                )
                .append("in", "\$\$match.$targetField")
        )
    }

    fun lookupObject(lookupArray: String, itemIdField: String): Document {
        return Document(
            "\$let", Document()
                .append(
                    "vars", Document(
                        "match", Document(
                            "\$arrayElemAt", listOf(
                                Document(
                                    "\$filter", Document()
                                        .append("input", "\$$lookupArray")
                                        .append("as", "el")
                                        .append("cond", Document("\$eq", listOf("\$\$el._id", "\$\$item.$itemIdField")))
                                ), 0
                            )
                        )
                    )
                )
                .append("in", "\$\$match")
        )
    }

    fun getMealCardProjectionStages(ingredientsField: String = "ingredients"): List<AggregationOperation> {
        val lookupIngredients = Aggregation.lookup("ingredients", "$ingredientsField.ingredient", "_id", "temp_ingredients")
        val lookupUnits = Aggregation.lookup("units", "$ingredientsField.unit", "_id", "temp_units")

        val projectStage = Aggregation.stage(
            Document(
                "\$project", Document()
                    .append("id", Document("\$toString", "\$_id"))
                    .append("name", 1)
                    .append("rating", 1)
                    .append("time", 1)
                    .append("difficulty", 1)
                    .append("images", 1)
                    .append("instructions", 1)
                    .append("matchingRatio", 1)
                    .append("relevanceScore", 1)

                    .append(
                        ingredientsField, Document(
                            "\$map", Document()
                                .append("input", "\$$ingredientsField")
                                .append("as", "item")
                                .append(
                                    "in", Document()
                                        .append("name", "\$\$item.name")
                                        .append("amount", "\$\$item.amount")
                                        .append(
                                            "ingredientName",
                                            lookupField("temp_ingredients", "ingredient", "name")
                                        )
                                        .append(
                                            "ingredientCategory",
                                            lookupField("temp_ingredients", "ingredient", "category")
                                        )
                                        .append(
                                            "unitAbbreviation",
                                            lookupField("temp_units", "unit", "abbreviation"),
                                        )
                                        .append("unitFullName", lookupField("temp_units", "unit", "fullName"))
                                )
                        )
                    )
            )
        )
        return listOf(lookupIngredients, lookupUnits, projectStage)
    }
}
