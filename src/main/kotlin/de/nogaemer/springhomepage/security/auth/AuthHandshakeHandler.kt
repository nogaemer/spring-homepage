package de.nogaemer.springhomepage.security.auth

import org.springframework.http.server.ServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal

class AuthHandshakeHandler : DefaultHandshakeHandler() {
    override fun determineUser(req: ServerHttpRequest, wsHandler: WebSocketHandler, attributes: MutableMap<String, Any>): Principal? {
        return attributes["user"] as? Principal
    }
}
