package org.dthv.linkforge.app.config

import org.dthv.linkforge.app.repository.LinkMappingRepository
import org.dthv.linkforge.app.repository.impl.InMemoryMappingRepository
import org.dthv.linkforge.app.repository.impl.RedisMappingRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
class StoreConfig(val appConfig: AppConfig, val redis: StringRedisTemplate) {
    @Bean
    fun getMappingStore(): LinkMappingRepository {
        if (appConfig.redis == null) return InMemoryMappingRepository()
        return RedisMappingRepository(redis)
    }
}