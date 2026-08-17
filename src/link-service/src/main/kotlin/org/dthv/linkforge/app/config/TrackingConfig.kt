package org.dthv.linkforge.app.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "link.forge.tracking")
data class TrackingConfig(val exchange: String)
