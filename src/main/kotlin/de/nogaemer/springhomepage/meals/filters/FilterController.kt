package de.nogaemer.springhomepage.meals.filters

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/filters")
class FilterController(
    val filterService: FilterService
) {

    @GetMapping("/all")
    fun getFilters(): ResponseEntity<FilterResponse> {
        return ResponseEntity.ok().body(filterService.getFilters())
    }

}