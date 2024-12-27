package de.nogaemer.springhomepage.main.images

import org.json.JSONObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/images")
class ImageController(
    val imageService: ImageService
) {

     @PostMapping("/upload")
     fun uploadImage(
         @RequestBody imageDto: ImageDto
     ): ResponseEntity<Image> {
         return ResponseEntity.ok(imageService.uploadImage(imageDto.base64))
     }


}