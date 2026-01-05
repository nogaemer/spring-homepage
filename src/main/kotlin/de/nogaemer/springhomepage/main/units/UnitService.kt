package de.nogaemer.springhomepage.main.units

import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Order
import org.springframework.data.domain.Sort.by
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.`when`
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull


@Service
class UnitService(
    val unitRepository: UnitRepository,
    val mongoTemplate: MongoTemplate
) {

    fun saveUnit(unit: IngredientUnit): IngredientUnit {
        return unitRepository.save(unit)
    }

    fun saveUnits(units: List<IngredientUnit>): List<IngredientUnit> {
        return unitRepository.saveAll(units)
    }

    fun removeUnit(unit: IngredientUnit) {
        unitRepository.delete(unit)
    }

    fun getUnits(limit: Int, offset: Int = 0, query: String): MutableList<IngredientUnit> {
        val pageable: Pageable = PageRequest.of(offset, limit)

        if (query.isBlank()) {
            return unitRepository.findAll(pageable).content
        }

        val regex = ".*${query}.*"

        // Per-field match -> score \`1\` if matched, else \`0\`
        val abbreviationMatchScore = `when`({ _ ->
            Document(
                "\$or", listOf(
                    Document(
                        "\$regexMatch",
                        Document("input", "\$abbreviation")
                            .append("regex", regex)
                            .append("options", "i")
                    ),
                    Document(
                        "\$regexMatch",
                        Document("input", "\$abbreviationPlural")
                            .append("regex", regex)
                            .append("options", "i")
                    )
                )
            )
        }).then(4).otherwise(0)

        val nameMatchScore = `when`({ _ ->
            Document(
                "\$or", listOf(
                    Document(
                        "\$regexMatch",
                        Document("input", "\$fullName")
                            .append("regex", regex)
                            .append("options", "i")
                    ),
                    Document(
                        "\$regexMatch",
                        Document("input", "\$fullNamePlural")
                            .append("regex", regex)
                            .append("options", "i")
                    )
                )
            )
        }).then(4).otherwise(0)

        val descriptionMatchScore = `when`({ _ ->
            Document(
                "\$regexMatch",
                Document("input", "\$description")
                    .append("regex", regex)
                    .append("options", "i")
            )
        }).then(2).otherwise(0)

        val categoryMatchScore = `when`({ _ ->
            Document(
                "\$regexMatch",
                Document("input", "\$category")
                    .append("regex", regex)
                    .append("options", "i")
            )
        }).then(1).otherwise(0)

        // Sum all contributions into \`priority\`
        val priorityExpr = ArithmeticOperators.Add.valueOf(abbreviationMatchScore)
            .add(nameMatchScore)
            .add(descriptionMatchScore)
            .add(categoryMatchScore)

        val aggregation = newAggregation(
            // Pre-filter candidates
            match(
                Criteria().orOperator(
                    Criteria.where("abbreviation").regex(regex, "i"),
                    Criteria.where("abbreviationPlural").regex(regex, "i"),
                    Criteria.where("fullName").regex(regex, "i"),
                    Criteria.where("fullNamePlural").regex(regex, "i"),
                    Criteria.where("description").regex(regex, "i"),
                    Criteria.where("category").regex(regex, "i")
                )
            ),
            // Compute \`priority\`
            addFields().addField("priority").withValue(priorityExpr).build(),
            // Sort by \`priority\` desc, then \`name\` asc
            sort(by(Order.desc("priority"), Order.asc("fullName"))),
            // Pagination
            skip((offset * limit).toLong()),
            limit(limit.toLong())
        )

        val results = mongoTemplate.aggregate(aggregation, "units", IngredientUnit::class.java)
        return results.mappedResults.toMutableList()
    }

    fun findById(objectId: ObjectId): IngredientUnit? {
        return unitRepository.findById(objectId).getOrNull(0)
    }

    fun findById(objectId: String): IngredientUnit? {
        return unitRepository.findById(objectId).getOrNull()
    }
}