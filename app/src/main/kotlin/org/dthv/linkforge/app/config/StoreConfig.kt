package org.dthv.linkforge.app.config

import org.dthv.linkforge.app.repository.LinkMappingRepository
import org.dthv.linkforge.app.repository.impl.InMemoryMappingRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StoreConfig(val appConfig: AppConfig) {
    @Bean
    fun getMappingStore() : LinkMappingRepository {
        if (appConfig.redis == null) return InMemoryMappingRepository()
        TODO("RedisMappingStore")
    }
}