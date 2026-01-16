/*
 * WebSocket Configuration with STOMP Protocol
 * 
 * This configuration enables WebSocket messaging for real-time features like live search.
 * It integrates JWT authentication into the WebSocket handshake process to ensure only
 * authenticated users can establish WebSocket connections.
 */
package de.nogaemer.springhomepage.security.config

import de.nogaemer.springhomepage.security.auth.AuthHandshakeHandler
import de.nogaemer.springhomepage.security.auth.JwtHandshakeInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * Configures WebSocket messaging with STOMP protocol and JWT authentication.
 * 
 * Sets up message broker for /topic destinations, application prefix for /app messages,
 * and registers WebSocket endpoint /ws-search with JWT handshake validation.
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    val jwtHandshakeInterceptor: JwtHandshakeInterceptor
) : WebSocketMessageBrokerConfigurer {

    /**
     * Configures the message broker for routing messages.
     * 
     * Enables simple in-memory broker for /topic destinations (for broadcasting to subscribers)
     * and sets /app as the prefix for messages directed to application handlers.
     */
    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic")
        config.setApplicationDestinationPrefixes("/app")
    }

    /**
     * Registers STOMP endpoints for WebSocket connections with JWT authentication.
     * 
     * The /ws-search endpoint validates JWT tokens during the WebSocket handshake via
     * JwtHandshakeInterceptor. Only authenticated users can establish connections.
     * Uses AuthHandshakeHandler to associate authenticated user with the WebSocket session.
     */
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws-search")
            // JWT authentication interceptor validates token during handshake
            .addInterceptors(jwtHandshakeInterceptor)
            .setHandshakeHandler(AuthHandshakeHandler())
            .setAllowedOriginPatterns("*")
    }

}