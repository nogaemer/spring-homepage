/**
 * WebSocket controller for real-time search completion suggestions.
 *
 * Provides real-time autocomplete functionality over WebSocket connections.
 * Clients send partial search queries and receive instant completion suggestions.
 *
 * WebSocket endpoint: /app/suggest (message mapping)
 * Broadcast endpoint: /topic/suggestions (subscription)
 */
package de.nogaemer.springhomepage.main.search

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.stereotype.Controller

/**
 * DTO for search requests over WebSocket.
 *
 * @property query The partial search query to generate completions for
 */
data class SearchRequestDto(
    val query: String
)

/**
 * Controller handling WebSocket messages for search completions.
 *
 * The WebSocket connection requires JWT authentication via query parameter.
 * See JwtHandshakeInterceptor for authentication details.
 *
 * @property completionService Service generating search completions
 */
@Controller
class SearchWebSocketController(
    private val completionService: SearchCompletionService
) {

    /**
     * Generates and broadcasts search completions to all subscribers.
     *
     * WebSocket message flow:
     * 1. Client sends SearchRequestDto to /app/suggest
     * 2. Server processes query and generates completions
     * 3. Results are broadcast to /topic/suggestions
     * 4. All subscribed clients receive the suggestions
     *
     * Performance: Completions are generated server-side using MongoDB aggregation
     * for sub-100ms response times on typical meal databases.
     *
     * @param request The search request containing the query
     * @param headerAccessor Message headers including user authentication
     * @return List of SearchCompletion objects (empty list for blank queries)
     */
    @MessageMapping("/suggest")
    @SendTo("/topic/suggestions")
    fun provideCompletions(
        @Payload request: SearchRequestDto,
        headerAccessor: SimpMessageHeaderAccessor
    ): List<SearchCompletion> {

        if (request.query.isBlank()) {
            return emptyList()
        }

        return completionService.getCompletions(request.query)
    }
}

