package de.nogaemer.springhomepage.main.meals.import.backup

import de.nogaemer.springhomepage.main.meals.models.Meal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/api/v1/backup")
class ImportBackupController(
    val importBackupService: ImportBackupService
) {

    @PostMapping("/import")
    fun updateMeal(
        @RequestBody meals: List<BackupMealModel>
    ): ResponseEntity<List<Meal>> {
        return ResponseEntity<List<Meal>>(importBackupService.import(meals), HttpStatus.OK)
    }
}