package com.dthv.linkforge.trackingservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tracking")
data class AppConfig(val queueName: String)