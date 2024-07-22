package de.nogaemer.springhomepage.hosting

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RenderOnlineController {

    @GetMapping()
    fun render(): ResponseEntity<String> {
        return ResponseEntity.ok().body("Hello World")
    }
}