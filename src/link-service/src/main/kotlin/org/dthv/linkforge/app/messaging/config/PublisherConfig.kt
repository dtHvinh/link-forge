package org.dthv.linkforge.app.messaging.config

import org.dthv.linkforge.app.config.TrackingConfig
import org.springframework.amqp.core.DirectExchange
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PublisherConfig(val trackingConfig: TrackingConfig) {
    @Bean
    fun linkEventsExchange(): DirectExchange = DirectExchange(trackingConfig.exchange)
}