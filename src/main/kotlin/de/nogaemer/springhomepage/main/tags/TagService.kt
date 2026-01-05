package de.nogaemer.springhomepage.main.tags

import de.nogaemer.springhomepage.main.meals.models.Meal
import org.bson.Document
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Order
import org.springframework.data.domain.Sort.by
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.`when`
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service


@Service
class TagService(
    val tagRepository: TagRepository,
    val mongoTemplate: MongoTemplate
) {

    fun saveTag(tag: Tag): Tag {
        return tagRepository.save(tag)
    }

    fun saveTags(tags: List<Tag>): List<Tag> {
        return tagRepository.saveAll(tags)
    }

    fun removeTag(tag: Tag) {
        tagRepository.delete(tag)
    }

    fun addTagToMeal(tag: Tag, meal: Meal): Tag {
        if (meal.tags.contains(tag)) {
            return tag
        }

        saveTag(tag)

        mongoTemplate.update(Meal::class.java)
            .matching(Criteria.where("id").`is`(meal.id))
            .apply(Update().push("tags", tag))
            .first()

        return tag
    }

    fun addTagsToMeal(tags: List<Tag>, meal: Meal) {
        tags.forEach { tag ->
            addTagToMeal(tag, meal)
        }
    }

    fun removeTagFromMeal(tag: Tag, meal: Meal): Tag {
        if (!meal.tags.none{it.id == tag.id}) {
            return tag
        }

        mongoTemplate.update(Meal::class.java)
            .matching(Criteria.where("id").`is`(meal.id))
            .apply(Update().pull("tags", tag))
            .first()

        return saveTag(tag)
    }

    fun updateMealTags(originalMeal: Meal, tags: MutableList<Tag>) {
        val originalTags = originalMeal.tags

        val originalTagIds = originalTags.map { it.id }.toSet()
        val newTagIds = tags.map { it.id }.toSet()

        if (originalTagIds == newTagIds) return

        mongoTemplate.update(Meal::class.java)
            .matching(Criteria.where("id").`is`(originalMeal.id))
            .apply(Update().set("tags", tags))
            .first()

    }

    fun getTags(limit: Int, offset: Int = 0, query: String): MutableList<Tag> {
        val pageable: Pageable = PageRequest.of(offset, limit)

        if (query.isBlank()) {
            return tagRepository.findAll(pageable).content
        }

        val regex = ".*${query}.*"

        // Per-field match -> score \`1\` if matched, else \`0\`
        val nameMatchScore = `when`({ _ ->
            Document("\$regexMatch",
                Document("input", "\$name")
                    .append("regex", regex)
                    .append("options", "i")
            )
        }).then(3).otherwise(0)

        val descriptionMatchScore = `when`({ _ ->
            Document("\$regexMatch",
                Document("input", "\$description")
                    .append("regex", regex)
                    .append("options", "i")
            )
        }).then(2).otherwise(0)

        val typeMatchScore = `when`({ _ ->
            Document("\$regexMatch",
                Document("input", "\$type")
                    .append("regex", regex)
                    .append("options", "i")
            )
        }).then(1).otherwise(0)

        // Sum all contributions into \`priority\`
        val priorityExpr = ArithmeticOperators.Add.valueOf(nameMatchScore)
            .add(descriptionMatchScore)
            .add(typeMatchScore)

        val aggregation = newAggregation(
            // Pre-filter candidates
            match(
                Criteria().orOperator(
                    Criteria.where("name").regex(regex, "i"),
                    Criteria.where("type").regex(regex, "i"),
                    Criteria.where("description").regex(regex, "i")
                )
            ),
            // Compute \`priority\`
            addFields().addField("priority").withValue(priorityExpr).build(),
            // Sort by \`priority\` desc, then \`name\` asc
            sort(by(Order.desc("priority"), Order.asc("name"))),
            // Pagination
            skip((offset * limit).toLong()),
            limit(limit.toLong())
        )

        val results = mongoTemplate.aggregate(aggregation, "tags", Tag::class.java)
        return results.mappedResults.toMutableList()
    }
}