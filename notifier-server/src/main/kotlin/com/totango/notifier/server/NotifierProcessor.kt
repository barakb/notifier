package com.totango.notifier.server

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.Disposable
import reactor.kafka.receiver.KafkaReceiver

@Component
class NotifierProcessor(
    val subscribers: Subscribers,
    val receiver: KafkaReceiver<String, String>
) {

    val matcher : Matcher = Matcher()

    fun process(): Disposable {
        return receiver.receive()
            .doOnNext { record ->
                notifySubscribers(record.value(), subscribers) }
            .subscribe()
    }

    private fun notifySubscribers(msg: String, subscribers: Subscribers) {
        val notification = msg.trim().split(Regex("\\s+"))
        subscribers.all().stream().filter { subscription -> match(notification, subscription) }
            .forEach { subscription ->
                emit(subscription, msg)
            }
    }

    private fun emit(subscription: Subscription, notification: String) {
        try {
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