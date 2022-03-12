package com.totango.notifier.server

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult

@Controller
class NotifierController(
    val subscribers: Subscribers,
    val sender: KafkaSender<String, String>,
    val properties: NotifierServerProperties
) {

    @MessageMapping("notify")
    fun notify(notification: String): Mono<Void> {
        logger.info("got notification $notification")
        return send(notification).then()
    }


    @MessageMapping("oneway")
    fun oneway(notification: String): Mono<Void> {
        return send(notification).then()
    }

    @MessageMapping("shared/subscribe")
    fun subscribeShared(pattern : String): Flux<String> {
        logger.info("got a shared subscribe request, pattern: $pattern")
        return Flux.create({ emitter: FluxSink<String> ->
            addEmitter(pattern, emitter)
        }, FluxSink.OverflowStrategy.ERROR)
    }

    @MessageMapping("standalone/subscribe")
    fun subscribeStandalone(pattern : String): Flux<String> {
        logger.info("got a standalone subscribe request, pattern: $pattern")
        return Flux.create({ emitter: FluxSink<String> ->
            addEmitter(pattern, emitter)
        }, FluxSink.OverflowStrategy.ERROR)
    }

    private fun send(notification: String): Flux<SenderResult<Int>> =
        sender.send(Mono.just(SenderRecord.create(ProducerRecord(properties.topic, notification, notification), 1)))

    private fun addEmitter(pattern: String, emitter: FluxSink<String>) {
        val subscription = Subscription(pattern.trim().split(Regex("\\s+")), emitter)
        subscribers.add(subscription)
        emitter.onDispose { subscribers.remove(subscription) }
    }

    companion object {
        @Suppress("unused")
        var logger: Logger = LoggerFactory.getLogger(NotifierController::class.java)
    }
}