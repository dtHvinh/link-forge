package org.dthv.linkforge.app.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "link.forge")
data class AppConfig(val redis: String?, val domain: String)