package org.dthv.linkforge.app.service

interface TrackingService {
    fun record(code: String, originalUrl: String)
}