package com.dthv.linkforge.trackingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class TrackingServiceApplication

fun main(args: Array<String>) {
    runApplication<TrackingServiceApplication>(*args)
}
