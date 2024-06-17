package de.nogaemer.springhomepage.meals.notes

import de.nogaemer.springhomepage.security.config.JwtService
import jakarta.servlet.http.HttpServletRequest
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/v1/notes")
class NotesController(
    val jwtService: JwtService,
    val service: NoteService
) {

    @GetMapping("/{id}")
    fun getNote(
        @PathVariable id: String
    ): ResponseEntity<List<Note>> {
        return ResponseEntity.ok(service.findByMealId(ObjectId(id)))
    }

    @PostMapping
    fun createNote(
        @RequestBody note: Note,
        request: HttpServletRequest
    ): ResponseEntity<Note> {
        val user = jwtService.extractUserFromRequest(request)
            ?: throw RuntimeException("Note found")

        note.userId = user.id!!
        return ResponseEntity<Note>(service.create(note), HttpStatus.CREATED)
    }

    @DeleteMapping("/{id}")
    fun deleteNote(
        @PathVariable id: String
    ): ResponseEntity<*> {
        service.delete(ObjectId(id))
        return ResponseEntity.ok().build<String>()
    }
}
