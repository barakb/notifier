package com.totango.notifier.server

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.rsocket.RSocketProperties
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import reactor.core.publisher.Hooks
import java.util.concurrent.CancellationException


@SpringBootApplication
@EnableConfigurationProperties(NotifierServerProperties::class)
class NotifierApplication(val notifierProcessor: NotifierProcessor, val connectionProperties: RSocketProperties) {

    @EventListener(ApplicationReadyEvent::class)
    fun doSomethingAfterStartup() {
        val transport = connectionProperties.server.transport
        val port = connectionProperties.server.port
        val mappingPath = connectionProperties.server.mappingPath
        logger.info("Event server ready $transport $port $mappingPath ")
        logger.info("Zipkin             http://localhost:9411")
        logger.info("Jaeger             http://localhost:16686")
        notifierProcessor.process()
    }

    companion object {
        @Suppress("unused")
        var logger: Logger = LoggerFactory.getLogger(NotifierApplication::class.java)
    }
}

fun main(args: Array<String>) {
    Hooks.onErrorDropped { e: Throwable ->
        if (e is CancellationException || e.cause is CancellationException) {
			NotifierApplication.logger.trace("Operator called default onErrorDropped", e)
        } else {
			NotifierApplication.logger.error("Operator called default onErrorDropped", e)
        }
    }
    runApplication<NotifierApplication>(*args)
}




