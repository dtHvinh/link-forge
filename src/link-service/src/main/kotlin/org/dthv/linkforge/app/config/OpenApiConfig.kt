package org.dthv.linkforge.app.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun linkForgeOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("Link Forge API")
                .description("URL shortening service")
                .version("v1")
        )
}
