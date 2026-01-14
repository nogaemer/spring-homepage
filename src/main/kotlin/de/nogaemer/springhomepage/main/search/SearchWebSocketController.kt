package de.nogaemer.springhomepage.main.search

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.stereotype.Controller

data class SearchRequestDto(
    val query: String
)

@Controller
class SearchWebSocketController(
    private val completionService: SearchCompletionService
) {

    @MessageMapping("/suggest")
    @SendTo("/topic/suggestions")
    fun provideCompletions(
        @Payload request: SearchRequestDto, // Explicit annotation helps
        headerAccessor: SimpMessageHeaderAccessor // Use this instead of Principal directly
    ): List<SearchCompletion> {

//        val principal = headerAccessor.user

        if (request.query.isBlank()) {
            return emptyList()
        }

        return completionService.getCompletions(request.query)
    }
}

