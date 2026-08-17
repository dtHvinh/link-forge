package org.dthv.linkforge.app.messaging

interface MessagePublisher {
    fun send(exchange: String, route: String, message: Any)
}