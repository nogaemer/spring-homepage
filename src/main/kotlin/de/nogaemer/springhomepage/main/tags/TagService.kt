/**
 * Service layer for tag management with intelligent search and meal association.
 *
 * Provides CRUD operations for tags and handles their relationships with meals.
 * Implements MongoDB aggregation-based search with relevance ranking across
 * tag name, description, and type fields.
 */
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

/**
 * Service for tag operations with meal relationship management.
 *
 * Handles tag-meal associations where tags are embedded in meal documents.
 * Provides intelligent search with relevance scoring and direct MongoDB
 * operations for efficient tag array manipulation in meals.
 *
 * @property tagRepository Repository for tag CRUD operations
 * @property mongoTemplate MongoDB template for direct document updates and aggregations
 */
@Service
class TagService(
    val tagRepository: TagRepository,
    val mongoTemplate: MongoTemplate
) {

    /**
     * Saves a single tag to the database.
     *
     * @param tag The tag to save
     * @return The saved tag with generated ID if new
     */
    fun saveTag(tag: Tag): Tag {
        return tagRepository.save(tag)
    }

    /**
     * Saves multiple tags in bulk.
     *
     * @param tags List of tags to save
     * @return List of saved tags with generated IDs
     */
    fun saveTags(tags: List<Tag>): List<Tag> {
        return tagRepository.saveAll(tags)
    }

    /**
     * Removes a tag from the database.
     *
     * Note: This does NOT remove the tag from meal documents.
     * Consider updating meals that reference this tag before deletion.
     *
     * @param tag The tag to delete
     */
    fun removeTag(tag: Tag) {
        tagRepository.delete(tag)
    }

    /**
     * Associates a tag with a meal by adding it to the meal's tags array.
     *
     * Uses MongoDB $push operation to add the tag to the meal document.
     * Prevents duplicate tags by checking if tag already exists in meal.
     * Saves tag to database if it doesn't exist yet.
     *
     * ## MongoDB Operation
     * ```javascript
     * db.meals.updateOne(
     *   { _id: meal.id },
     *   { $push: { tags: tag } }
     * )
     * ```
     *
     * @param tag The tag to add to the meal
     * @param meal The meal to receive the tag
     * @return The tag (unchanged if already present in meal)
     */
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

    /**
     * Associates multiple tags with a meal.
     *
     * Iterates through tags and adds each one to the meal.
     * Each tag is checked for duplicates before adding.
     *
     * @param tags List of tags to add
     * @param meal The meal to receive the tags
     */
    fun addTagsToMeal(tags: List<Tag>, meal: Meal) {
        tags.forEach { tag ->
            addTagToMeal(tag, meal)
        }
    }

    /**
     * Removes a tag from a meal's tags array.
     *
     * Uses MongoDB $pull operation to remove the tag from the meal document.
     * The tag itself remains in the tags collection.
     *
     * ## MongoDB Operation
     * ```javascript
     * db.meals.updateOne(
     *   { _id: meal.id },
     *   { $pull: { tags: tag } }
     * )
     * ```
     *
     * @param tag The tag to remove from the meal
     * @param meal The meal to remove the tag from
     * @return The tag (saved to ensure it exists in database)
     */
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

    /**
     * Updates a meal's complete tag set if it has changed.
     *
     * Compares original and new tag IDs to detect changes.
     * If tags differ, replaces entire tags array in one operation.
     * More efficient than individual add/remove when many tags change.
     *
     * ## MongoDB Operation
     * ```javascript
     * db.meals.updateOne(
     *   { _id: meal.id },
     *   { $set: { tags: newTags } }
     * )
     * ```
     *
     * @param originalMeal The meal with current tags
     * @param tags The new complete list of tags
     */
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

    /**
     * Retrieves tags with intelligent search and relevance ranking.
     *
     * Uses MongoDB aggregation pipeline to implement a sophisticated scoring algorithm:
     * - Name match: 3 points
     * - Description match: 2 points
     * - Type match: 1 point
     *
     * Results are sorted by computed priority (descending) then name (ascending).
     * Empty query returns all tags sorted by name with pagination.
     *
     * ## Search Algorithm
     * The aggregation pipeline:
     * 1. Pre-filters documents matching query in any field (case-insensitive)
     * 2. Computes priority score based on field matches
     * 3. Sorts by priority descending, then name ascending
     * 4. Applies pagination with skip and limit
     *
     * ## Performance Considerations
     * - Uses regex with case-insensitive matching
     * - Pre-filter reduces scoring computation to candidates only
     * - Single aggregation pipeline minimizes database round-trips
     *
     * @param limit Maximum number of results to return
     * @param offset Page offset for pagination (multiplied by limit for skip)
     * @param query Search query string (empty string returns all results)
     * @return List of tags sorted by relevance
     */
    fun getTags(limit: Int, offset: Int = 0, query: String): MutableList<Tag> {
        val pageable: Pageable = PageRequest.of(offset, limit)

        if (query.isBlank()) {
            return tagRepository.findAll(pageable).content
        }

        val regex = ".*${query}.*"

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

        val priorityExpr = ArithmeticOperators.Add.valueOf(nameMatchScore)
            .add(descriptionMatchScore)
            .add(typeMatchScore)

        val aggregation = newAggregation(
            match(
                Criteria().orOperator(
                    Criteria.where("name").regex(regex, "i"),
                    Criteria.where("type").regex(regex, "i"),
                    Criteria.where("description").regex(regex, "i")
                )
            ),
            addFields().addField("priority").withValue(priorityExpr).build(),
            sort(by(Order.desc("priority"), Order.asc("name"))),
            skip((offset * limit).toLong()),
            limit(limit.toLong())
        )

        val results = mongoTemplate.aggregate(aggregation, "tags", Tag::class.java)
        return results.mappedResults.toMutableList()
    }
}