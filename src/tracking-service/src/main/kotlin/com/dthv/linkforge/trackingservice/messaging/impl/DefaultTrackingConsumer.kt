package com.dthv.linkforge.trackingservice.messaging.impl

import com.dthv.linkforge.trackingservice.messaging.TrackingConsumer
import com.dthv.linkforge.trackingservice.repository.TrackingDataStore
import org.dthv.linkforge.domain.messaging.events.LinkAccessEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DefaultTrackingConsumer(val trackingDataStore: TrackingDataStore) : TrackingConsumer {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    override fun onLinkAccess(event: LinkAccessEvent) {
        logger.debug("Event received {}", event)
        trackingDataStore.hit(event.code)
    }
}