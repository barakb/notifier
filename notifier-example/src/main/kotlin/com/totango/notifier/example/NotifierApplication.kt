package com.totango.notifier.example

import com.totango.notifier.client.Notifier
import com.totango.notifier.client.SubscriberMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import reactor.core.Disposable

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
        logger.debug("subscribe on *")
        val disposable: Disposable = notifier.subscribe("*").doOnNext{
            logger.debug("shared     subscriber got $it")
        }.subscribe()

        logger.debug("subscribing standalone subscriber on FOO *")
        val standAloneDisposable: Disposable = notifier.subscribe("FOO *", SubscriberMode.Standalone).doOnNext{
            logger.debug("standalone subscriber got $it")
        }.subscribe()


        logger.debug("notify FOO")
        notifier.notify("foo").block()

        logger.debug("oneway A ONE WAY")
        notifier.oneway("a one way").block()

        logger.debug("notify FOO AND BAR")
        notifier.notify("foo and bar").block()

        logger.debug("sleeping one second")
        Thread.sleep(1000)
        disposable.dispose()
        standAloneDisposable.dispose()
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
