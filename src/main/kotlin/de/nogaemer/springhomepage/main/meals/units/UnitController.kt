package de.nogaemer.springhomepage.main.meals.units

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
@RequestMapping("/api/v1/units")
class UnitController {
    @Autowired
    val service: UnitService? = null

    @GetMapping
    fun getUnits(
        @RequestParam(
            value = "limit",
            defaultValue = "10"
        ) limit: Int,
        @RequestParam(
            value = "query",
            defaultValue = ""
        ) query: String
    ): ResponseEntity<List<IngredientUnit>> {
        return ResponseEntity<List<IngredientUnit>>(service?.getUnits(limit,0, query), HttpStatus.OK)
    }

    @PostMapping
    fun createUnit(
        @RequestBody unit: IngredientUnit
    ): ResponseEntity<IngredientUnit>{
        return ResponseEntity<IngredientUnit>(service?.saveUnit(unit), HttpStatus.OK)
    }

}