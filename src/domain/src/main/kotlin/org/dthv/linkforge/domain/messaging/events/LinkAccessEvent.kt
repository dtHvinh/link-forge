package org.dthv.linkforge.domain.messaging.events

import kotlin.time.Instant

data class LinkAccessEvent(
    val code: String,
    val originalUrl: String,
    val timestamp: Instant,
)
