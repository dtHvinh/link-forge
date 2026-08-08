package org.dthv.linkforge.app.config

import org.dthv.linkforge.app.annotations.NotVersioning
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.function.Predicate

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix(
            "/api/v1",
            prefixApplicableEndpoints()
        )
    }

    fun prefixApplicableEndpoints(): Predicate<Class<*>> {
        return {
            it.isAnnotationPresent(RestController::class.java)
                    && !it.isAnnotationPresent(NotVersioning::class.java)
                    && !it.simpleName.contains("OpenApi", true)
        }
    }
}