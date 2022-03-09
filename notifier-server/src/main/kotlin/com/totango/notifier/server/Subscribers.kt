package com.totango.notifier.server

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Component
class Subscribers {
    private val values: ConcurrentMap<String, Subscription> = ConcurrentHashMap()

    fun add(subscription: Subscription) {
        logger.info("adding subscription $subscription")
        values[subscription.uid] = subscription
    }

    fun remove(subscription: Subscription) {
        logger.info("removing subscription $subscription")
        values.remove(subscription.uid, subscription)
    }

    fun all(): Collection<Subscription> = values.values


    companion object {
        @Suppress("unused")
        var logger: Logger = LoggerFactory.getLogger(Subscribers::class.java)
    }
}