package org.dthv.linkforge.app.web.health

import org.dthv.linkforge.app.web.health.dto.HealthResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("health")
class HealthController {

    @GetMapping("status")
    fun getHealth(): HealthResponse = HealthResponse("Good")
}