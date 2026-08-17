package org.dthv.linkforge.app.messaging.queue

import org.dthv.linkforge.app.config.TrackingConfig
import org.springframework.amqp.core.DirectExchange
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QueueConfig(val trackingConfig: TrackingConfig) {
    @Bean
    fun linkEventsExchange(): DirectExchange = DirectExchange(trackingConfig.exchange)
}