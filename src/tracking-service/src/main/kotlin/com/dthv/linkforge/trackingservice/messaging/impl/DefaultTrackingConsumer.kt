package com.dthv.linkforge.trackingservice.messaging.impl

import com.dthv.linkforge.trackingservice.messaging.TrackingConsumer
import org.dthv.linkforge.domain.messaging.events.LinkAccessEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DefaultTrackingConsumer : TrackingConsumer {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    override fun onLinkAccess(event: LinkAccessEvent) {
        logger.info("Event received {}", event)
    }
}