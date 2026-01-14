package de.nogaemer.springhomepage.security.config

import de.nogaemer.springhomepage.security.auth.AuthHandshakeHandler
import de.nogaemer.springhomepage.security.auth.JwtHandshakeInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    val jwtHandshakeInterceptor: JwtHandshakeInterceptor
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic")
        config.setApplicationDestinationPrefixes("/app")
    }

    // In WebSocketConfig.kt
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws-search")
            .addInterceptors(jwtHandshakeInterceptor) // <--- Add this
            .setHandshakeHandler(AuthHandshakeHandler())
            .setAllowedOriginPatterns("*")
    }

}