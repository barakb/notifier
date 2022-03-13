package com.totango.notifier.example

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.totango.notifier.client.Notifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import reactor.core.Disposable
import java.util.concurrent.TimeUnit


@SpringBootApplication
class NotifierApplication {

    @Bean
    fun runExample(notifier: Notifier): ApplicationRunner =
        ApplicationRunner {
            example(notifier)
        }

    data class CachedValue(val payload: String)

    private fun example(notifier: Notifier){
        val cache: Cache<String, CachedValue> = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(100)
            .build()
        cache.put("880:ACCOUNT1", CachedValue("ACCOUNT1"))
        cache.put("880:ACCOUNT2", CachedValue("ACCOUNT2"))
        cache.put("880:ACCOUNT3", CachedValue("ACCOUNT3"))

        val disposable: Disposable = notifier.subscribe("880 MODIFY ACCOUNT ?").doOnNext{ event ->
           val key = "${event.tokens[0]}:${event.tokens[3]}"
            logger.info("invalidating $key")
            cache.invalidate(key)
            logger.info("cache contains ${cache.asMap()}")
        }.retry().subscribe()

        logger.info("cache contains ${cache.asMap()}")

        notifier.notify("880 MODIFY ACCOUNT ACCOUNT1").block()

        notifier.oneway("880 MODIFY ACCOUNT ACCOUNT3").block()

        notifier.notify("880 MODIFY ACCOUNT ACCOUNT3").block()

        notifier.notify("881 MODIFY ACCOUNT ACCOUNT1").block()

        Thread.sleep(10000)
        logger.info("at end, cache contains ${cache.asMap()}")
        disposable.dispose()
        logger.debug("Done")

    }

    companion object {
        @Suppress("unused")
        var logger: Logger = LoggerFactory.getLogger(NotifierApplication::class.java)
    }
}

fun main(args: Array<String>) {
    runApplication<NotifierApplication>(*args)
}
