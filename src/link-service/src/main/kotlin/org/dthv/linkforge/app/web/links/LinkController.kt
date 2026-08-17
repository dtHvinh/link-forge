package org.dthv.linkforge.app.web.links

import io.lettuce.core.dynamic.annotation.Param
import org.dthv.linkforge.app.service.LinkService
import org.dthv.linkforge.app.web.links.dto.CreateLinkRequest
import org.dthv.linkforge.app.web.links.dto.CreateLinkResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("links")
class LinkController(val linkService: LinkService) {
    @PostMapping
    fun generateLink(@RequestBody request: CreateLinkRequest): CreateLinkResponse {
        val generatedLink = linkService.generateLink(request.link)
        return CreateLinkResponse(generatedLink)
    }
}