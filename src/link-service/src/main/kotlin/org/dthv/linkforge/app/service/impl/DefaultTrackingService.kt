package org.dthv.linkforge.app.service.impl

import org.dthv.linkforge.app.config.TrackingConfig
import org.dthv.linkforge.app.messaging.MessagePublisher
import org.dthv.linkforge.app.service.TrackingService
import org.dthv.linkforge.domain.messaging.events.LinkAccessEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpException
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import kotlin.time.Clock

@Service
class DefaultTrackingService(
    val messagePublisher: MessagePublisher,
    val trackingConfig: TrackingConfig,
) :
    TrackingService {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Async("trackingExecutor")
    override fun record(code: String, originalUrl: String) {
        try {
            messagePublisher.send(
                trackingConfig.exchange,
                trackingConfig.routingKey,
                LinkAccessEvent(code, originalUrl, Clock.System.now())
            )
        } catch (ex: AmqpException) {
            logger.error("Failed to publish message", ex)
        }
    }
}