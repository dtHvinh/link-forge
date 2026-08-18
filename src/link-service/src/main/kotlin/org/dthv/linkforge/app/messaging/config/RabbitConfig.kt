package org.dthv.linkforge.app.messaging.config

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {
    @Bean
    fun messageConverter(): MessageConverter = JacksonJsonMessageConverter()
}