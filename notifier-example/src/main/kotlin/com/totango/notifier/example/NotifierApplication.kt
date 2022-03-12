package com.totango.notifier.example

import com.totango.notifier.client.Notifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class NotifierApplication {

    @Bean
    fun runExample(notifier: Notifier): ApplicationRunner =
        ApplicationRunner {
            notify(notifier)
        }

    fun notify(notifier: Notifier){
//        val t = T()
//        t.exportProd()
//        t.exportEU()
        logger.info("subscribe on *")
        val disposable = notifier.subscribe("*").doOnNext{
            logger.info("subscriber got $it")
        }.subscribe()

        logger.info("notify FOO")
        notifier.notify("foo").block()

        logger.info("oneway A ONE WAY")
        notifier.oneway("a one way").block()

        logger.info("notify FOO")
        notifier.notify("foo").block()

        logger.info("sleeping one second")
        Thread.sleep(1000)
        disposable.dispose()
        logger.info("Done")

    }

    companion object {
        @Suppress("unused")
        var logger: Logger = LoggerFactory.getLogger(NotifierApplication::class.java)
    }
}

fun main(args: Array<String>) {
    runApplication<NotifierApplication>(*args)
}
