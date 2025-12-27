package de.nogaemer.springhomepage.main.meals.tags

import de.nogaemer.springhomepage.main.meals.dto.MealDto
import de.nogaemer.springhomepage.main.meals.models.Meal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tags")
class TagController {
    @Autowired
    val service: TagService? = null

    @GetMapping
    fun getTags(
        @RequestParam(
            value = "limit",
            defaultValue = "10"
        ) limit: Int,
        @RequestParam(
            value = "query",
            defaultValue = ""
        ) query: String
    ): ResponseEntity<List<Tag>> {
        return ResponseEntity<List<Tag>>(service?.getTags(limit,0, query), HttpStatus.OK)
    }

    @PostMapping
    fun createTag(
        @RequestBody tag: Tag
    ): ResponseEntity<Tag>{
        return ResponseEntity<Tag>(service?.saveTag(tag), HttpStatus.OK)
    }

}