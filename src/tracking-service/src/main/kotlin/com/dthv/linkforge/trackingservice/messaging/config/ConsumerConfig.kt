package com.dthv.linkforge.trackingservice.messaging.config

import com.dthv.linkforge.trackingservice.config.AppConfig
import com.dthv.linkforge.trackingservice.config.TrackingConfig
import com.dthv.linkforge.trackingservice.messaging.TrackingConsumer
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class ConsumerConfig(val appConfig: AppConfig, val trackingConfig: TrackingConfig) {
    @Bean
    fun linkEventsExchange(): DirectExchange = DirectExchange(trackingConfig.exchange)

    @Bean
    fun linkAccessQueue(): Queue = Queue(appConfig.queueName)

    @Bean
    fun linkAccessBinding(linkAccessQueue: Queue, linkEventsExchange: DirectExchange): Binding =
        BindingBuilder.bind(linkAccessQueue).to(linkEventsExchange).with(trackingConfig.routingKey)

    @Bean
    fun linkAccessListenerContainer(
        connectionFactory: ConnectionFactory,
        linkAccessQueue: Queue,
        linkAccessConsumer: TrackingConsumer,
        messageConverter: MessageConverter,
        rabbitTemplate: RabbitTemplate
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueues(linkAccessQueue)
        val adapter = MessageListenerAdapter(linkAccessConsumer, messageConverter)
        adapter.setDefaultListenerMethod(TrackingConsumer::onLinkAccess.name)
        container.setMessageListener(adapter)

        val recoverer = RepublishMessageRecoverer(rabbitTemplate, "", "${appConfig.queueName}.dlq")
        val retryInterceptor = RetryInterceptorBuilder.stateless()
            .maxRetries(3)
            .backOffOptions(1000, 2.0, 10000)
            .recoverer(recoverer)
            .build()
        container.setAdviceChain(retryInterceptor)

        return container
    }
}