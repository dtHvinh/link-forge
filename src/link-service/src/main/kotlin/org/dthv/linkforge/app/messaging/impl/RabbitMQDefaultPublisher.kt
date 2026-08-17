package org.dthv.linkforge.app.messaging.impl

import org.dthv.linkforge.app.messaging.MessagePublisher
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class RabbitMQDefaultPublisher(val rabbitTemplate: RabbitTemplate) : MessagePublisher {
    override fun send(exchange: String, route: String, message: Any) {
        rabbitTemplate.convertAndSend(exchange, route, message)
    }
}