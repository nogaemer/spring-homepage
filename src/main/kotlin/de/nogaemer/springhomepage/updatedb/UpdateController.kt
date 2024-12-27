package de.nogaemer.springhomepage.updatedb

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/v1/updateDB")
class UpdateController(val service: UpdateService?) {

    @GetMapping
    fun getMealsByName(
        @RequestParam(
            value = "updateId",
        ) updateID: Int
    ): ResponseEntity<Any> {
        return ResponseEntity<Any>(service?.update(updateID), HttpStatus.OK)
    }
}