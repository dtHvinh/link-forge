package org.dthv.linkforge.app.web.redirect

import org.dthv.linkforge.app.service.LinkService
import org.springframework.http.HttpStatus.FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.status
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.net.URI

@Controller
class RedirectController(
    private val service: LinkService,
) {
    @GetMapping("/{code:[a-zA-Z0-9]{3,32}}")
    fun redirect(@PathVariable code: String): ResponseEntity<Void> {
        val originalUrl = service.resolve(code)
        return status(FOUND)
            .location(URI.create(originalUrl))
            .build()
    }
}