package com.dthv.linkforge.trackingservice.messaging

import org.dthv.linkforge.domain.messaging.events.LinkAccessEvent

interface TrackingConsumer {
    fun onLinkAccess(event: LinkAccessEvent)
}