package de.nogaemer.springhomepage.main.meals

import de.nogaemer.springhomepage.main.meals.dto.UnifiedMealSearchRequest
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationResults

class UnifiedMealSearchServiceTest {

    @Test
    fun `lookup pipeline contains userId filter when userIds provided`() {
        val mongoTemplate = mock<MongoTemplate>()

        // capture the Aggregation passed into aggregate(...)
        val aggregationCaptor = argumentCaptor<Aggregation>()
        whenever(mongoTemplate.aggregate(aggregationCaptor.capture(), eq("meals"), eq(Document::class.java)))
            .thenReturn(AggregationResults(emptyList<Document>(), Document()))

        val service = UnifiedMealSearchService(mongoTemplate)

        val userId = "669e8cc12c91a20e9de8bdee"
        val request = UnifiedMealSearchRequest(
            name = null,
            tagIds = null,
            ingredients = null,
            userIds = listOf(userId),
            minUserRating = null,
            requireUserRatingMatch = false,
            sortBy = UnifiedMealSearchRequest.SortBy.RATING,
            skip = 0,
            limit = 10,
            minTime = null,
            maxTime = null,
            minIngredientMatch = null
        )

        service.search(request)

        val agg = aggregationCaptor.firstValue
        assertNotNull(agg)

        // try to read the private 'operations' field from Aggregation (reflection) to inspect composed stages
        val opsField = agg.javaClass.getDeclaredField("operations")
        opsField.isAccessible = true
        val ops = opsField.get(agg) as List<*>

        // textual check: join operation.toString() and assert presence of ratings and userId
        val opsString = ops.joinToString("||") { it.toString() }

        assertTrue(opsString.contains("ratings") || opsString.contains("userRatings"), "expected lookup for ratings to be present in pipeline: $opsString")

        // ensure the provided userId (as hex) is present somewhere in the pipeline string
        assertTrue(opsString.contains(userId) || opsString.contains(ObjectId(userId).toHexString()), "expected userId to appear in lookup pipeline: $opsString")
    }
}

