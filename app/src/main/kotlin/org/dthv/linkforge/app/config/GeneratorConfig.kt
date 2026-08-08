package org.dthv.linkforge.app.config

import org.dthv.linkforge.app.service.CodeGenerateService
import org.dthv.linkforge.app.service.impl.Base62RedisCodeGeneratorService
import org.dthv.linkforge.app.service.impl.RandomCodeGeneratorService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
class GeneratorConfig(val appConfig: AppConfig, val redis: StringRedisTemplate) {
    @Bean
    fun getLinkGenerator() : CodeGenerateService {
        if (appConfig.redis != null) return Base62RedisCodeGeneratorService(redis)
        return RandomCodeGeneratorService()
    }
}