package com.totango.notifier.server

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.sleuth.Span
import org.springframework.cloud.sleuth.SpanAndScope
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.annotation.NewSpan
import org.springframework.cloud.sleuth.propagation.Propagator
import org.springframework.stereotype.Component
import reactor.core.Disposable
import reactor.kafka.receiver.KafkaReceiver


@Component
class NotifierProcessor(
    val subscribers: Subscribers,
    val receiver: KafkaReceiver<String, String>,
    val tracer: Tracer,
    val extractor: Propagator.Getter<ConsumerRecord<*, *>>,
    val propagator: Propagator
) {

    val matcher: Matcher = Matcher()

    fun process(): Disposable {
        return receiver.receive()
            .doOnNext { record ->
                val builder: Span.Builder = propagator.extract(record, extractor)
                val childSpan: Span = builder.name("notifySubscribers").tag("event", record.value()).start()
                val spanAndScope = SpanAndScope(childSpan, tracer.withSpan(childSpan))
                spanAndScope.use {
                    notifySubscribers(record.value(), subscribers)
                }
//                }
            }
            .subscribe()
    }

    @NewSpan
    fun notifySubscribers(msg: String, subscribers: Subscribers) {
        val notification = msg.trim().split(Regex("\\s+"))
        subscribers.all().filter { subscription -> match(notification, subscription) }
            .forEach { subscription ->
                emit(subscription, msg)
            }
    }

    @NewSpan
    fun emit(subscription: Subscription, notification: String) {
        try {
            logger.debug("shared dispatch notification [$notification] to subscriber ${subscription.uid} because of subscription ${subscription.pattern}")
            subscription.emitter.next(notification)
        } catch (e: Exception) {
            logger.error(e.toString(), e)
            // todo on other thread
            subscription.emitter.complete()
            subscribers.remove(subscription)
        }
    }

    private fun match(notification: List<String>, subscription: Subscription): Boolean =
        matcher.match(subscription.pattern, notification)

    companion object {
        @Suppress("unused")
        var logger: Logger = LoggerFactory.getLogger(NotifierProcessor::class.java)
    }
}