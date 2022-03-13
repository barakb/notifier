[![build](https://github.com/barakb/notifier/actions/workflows/build.yml/badge.svg)](https://github.com/barakb/notifier/actions/workflows/build.yml)
[![Renovate enabled](https://img.shields.io/badge/renovate-enabled-brightgreen.svg)](https://renovatebot.com/)

### The goal of this project
To provide an easy way for sending and receiving notifications between processes

### The structure of this project
1. [A Notifier client spring boot starter](https://github.com/barakb/notifier/tree/master/notifier-client-starter) -- a spring boot started that when include in a spring boot project automatically create a Notifier client
configuration is done with one property `notifier.server.url` that should set the url (possible behind load balancer) of the Notifier server, for example `notifier.server.url=ws://localhost:6565/rsocket`
2. [A Notifier server](https://github.com/barakb/notifier/tree/master/notifier-server) -- a server that use kafka to manage subscription and notifications
configuration is done using the following properties
```properties
# suppress inspection "UnusedProperty" for whole file
spring.rsocket.server.port=6565
spring.rsocket.server.transport=websocket
spring.rsocket.server.mapping-path=/rsocket


notifier.server.kafka.bootstrapServers=localhost:29092
notifier.server.kafka.topic=notifications
```
the rsocket params should match the clients `notifier.server.url`

3. [notifier local env](https://github.com/barakb/notifier/tree/master/notifier-local-env) a docker compose with kafka zipkin jaeger and open telemetry collector
4. [An example](https://github.com/barakb/notifier/tree/master/notifier-example) -- a spring boot application that uses the notifier client started to send and receive notifications 


#### Using the Notifier
The [Notifier](https://github.com/barakb/notifier/blob/master/notifier-client-starter/src/main/kotlin/com/totango/notifier/client/Notifier.kt) interface is very simple

```kotlin
interface Notifier {
    fun notify(notification: String) : Mono<Void>
    fun oneway(notification: String): Mono<Void>
    fun subscribe(pattern: String, subscriberMode: SubscriberMode = SubscriberMode.Shared): Flux<Event>
}
```

Sending notification can be done in 2 ways
1. sending and waiting for the notification to be written to the kafka topic
```kotlin
notifier.notify("foo")
```
2. sending and not waiting
```kotlin
notifier.oneway("a one way")
```

#### Subscription modes
when subscribing the client can choose between 2 modes
1. A shared mode, in which all the subscribers share the same kafka consumer, the advantage of this mode is that it is very lightweight, the downside 
is that a slow subscribers are [immediately disconnected](https://github.com/barakb/notifier/blob/master/notifier-server/src/main/kotlin/com/totango/notifier/server/NotifierController.kt#L42) otherwise other subscribers will get the notification late (no back-pressure).
This is the default mode.
```kotlin
val disposable: Disposable = notifier.subscribe("*").doOnNext{
    logger.debug("shared     subscriber got $it")
}.subscribe()
```
2. stand-alone subscription on the other hand is a bit heavier because each subscriber get its own Kafka reactive consumer, this subscriber gets events as fast as it consumes them but not faster (back-pressure is working).
```kotlin
val standAloneDisposable: Disposable = notifier.subscribe("FOO *", SubscriberMode.Standalone).doOnNext{
    logger.debug("standalone subscriber got $it")
}.subscribe()
```


#### An example, cleaning Caffeine cache using notifications

```kotlin
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
```

the output of the client should be something like:
```shell
2022-03-13 21:18:29.682  INFO [notifier-client,,] 57013 --- [           main] c.t.n.example.NotifierApplication        : cache contains {880:ACCOUNT2=CachedValue(payload=ACCOUNT2), 880:ACCOUNT1=CachedValue(payload=ACCOUNT1), 880:ACCOUNT3=CachedValue(payload=ACCOUNT3)}
2022-03-13 21:18:30.181  INFO [notifier-client,7eafddc7d9f9fe8e,7eafddc7d9f9fe8e] 57013 --- [actor-tcp-nio-2] c.t.n.example.NotifierApplication        : invalidating 880:ACCOUNT1
2022-03-13 21:18:30.184  INFO [notifier-client,7eafddc7d9f9fe8e,7eafddc7d9f9fe8e] 57013 --- [actor-tcp-nio-2] c.t.n.example.NotifierApplication        : cache contains {880:ACCOUNT2=CachedValue(payload=ACCOUNT2), 880:ACCOUNT3=CachedValue(payload=ACCOUNT3)}
2022-03-13 21:18:30.185  INFO [notifier-client,7eafddc7d9f9fe8e,7eafddc7d9f9fe8e] 57013 --- [actor-tcp-nio-2] c.t.n.example.NotifierApplication        : invalidating 880:ACCOUNT3
2022-03-13 21:18:30.186  INFO [notifier-client,7eafddc7d9f9fe8e,7eafddc7d9f9fe8e] 57013 --- [actor-tcp-nio-2] c.t.n.example.NotifierApplication        : cache contains {880:ACCOUNT2=CachedValue(payload=ACCOUNT2)}
2022-03-13 21:18:30.188  INFO [notifier-client,7eafddc7d9f9fe8e,7eafddc7d9f9fe8e] 57013 --- [actor-tcp-nio-2] c.t.n.example.NotifierApplication        : invalidating 880:ACCOUNT3
2022-03-13 21:18:30.188  INFO [notifier-client,7eafddc7d9f9fe8e,7eafddc7d9f9fe8e] 57013 --- [actor-tcp-nio-2] c.t.n.example.NotifierApplication        : cache contains {880:ACCOUNT2=CachedValue(payload=ACCOUNT2)}
2022-03-13 21:18:40.200  INFO [notifier-client,,] 57013 --- [           main] c.t.n.example.NotifierApplication        : at end, cache contains {880:ACCOUNT2=CachedValue(payload=ACCOUNT2)}
2022-03-13 21:18:40.201 DEBUG [notifier-client,,] 57013 --- [           main] c.t.n.example.NotifierApplication        : Done
```

and the server

```shell
2022-03-13 21:21:53.019 DEBUG [notifier,,] 57042 --- [           main] c.t.notifier.server.NotifierApplication  : Event server ready WEBSOCKET 6565 /rsocket 
2022-03-13 21:21:53.019 DEBUG [notifier,,] 57042 --- [           main] c.t.notifier.server.NotifierApplication  : Zipkin             http://localhost:9411
2022-03-13 21:21:53.019 DEBUG [notifier,,] 57042 --- [           main] c.t.notifier.server.NotifierApplication  : Jaeger             http://localhost:16686
2022-03-13 21:22:00.134 DEBUG [notifier,b7621673249fbe5e,24ac47e0ddea97fb] 57042 --- [ctor-http-nio-2] c.t.notifier.server.NotifierController   : got a shared subscribe request, pattern: [880 MODIFY ACCOUNT ?]
2022-03-13 21:22:00.161 DEBUG [notifier,b7621673249fbe5e,24ac47e0ddea97fb] 57042 --- [ctor-http-nio-2] com.totango.notifier.server.Subscribers  : adding a shared subscription Subscription(pattern=[880, MODIFY, ACCOUNT, ?], emitter=FluxSink(ERROR), uid=c0d0b865-d1cd-40de-a076-81364badfd9d)
2022-03-13 21:22:00.171 DEBUG [notifier,b2fb5a2d3a242b61,3fc22abec39fa2e0] 57042 --- [ctor-http-nio-2] c.t.notifier.server.NotifierController   : got notification 880 MODIFY ACCOUNT ACCOUNT1
2022-03-13 21:22:00.281 DEBUG [notifier,b2fb5a2d3a242b61,85878a9d356bc44d] 57042 --- [otifier-group-1] c.t.notifier.server.NotifierProcessor    : shared dispatch notification [880 MODIFY ACCOUNT ACCOUNT1] to subscriber c0d0b865-d1cd-40de-a076-81364badfd9d because of subscription [880, MODIFY, ACCOUNT, ?]
2022-03-13 21:22:00.302 DEBUG [notifier,7ddd2fba4d1681bf,bdacd00dcd221c3b] 57042 --- [ctor-http-nio-2] c.t.notifier.server.NotifierController   : got notification 880 MODIFY ACCOUNT ACCOUNT3
2022-03-13 21:22:00.311 DEBUG [notifier,dfeb13611af19fb9,48b16aef9c28566d] 57042 --- [otifier-group-1] c.t.notifier.server.NotifierProcessor    : shared dispatch notification [880 MODIFY ACCOUNT ACCOUNT3] to subscriber c0d0b865-d1cd-40de-a076-81364badfd9d because of subscription [880, MODIFY, ACCOUNT, ?]
2022-03-13 21:22:00.317 DEBUG [notifier,7ddd2fba4d1681bf,f722e0a826cb207c] 57042 --- [otifier-group-1] c.t.notifier.server.NotifierProcessor    : shared dispatch notification [880 MODIFY ACCOUNT ACCOUNT3] to subscriber c0d0b865-d1cd-40de-a076-81364badfd9d because of subscription [880, MODIFY, ACCOUNT, ?]
2022-03-13 21:22:00.323 DEBUG [notifier,7640da0a5858e66e,3b50eb851e05985e] 57042 --- [ctor-http-nio-2] c.t.notifier.server.NotifierController   : got notification 881 MODIFY ACCOUNT ACCOUNT1
2022-03-13 21:22:10.332 DEBUG [notifier,b7621673249fbe5e,24ac47e0ddea97fb] 57042 --- [ctor-http-nio-2] com.totango.notifier.server.Subscribers  : removing a shared subscription Subscription(pattern=[880, MODIFY, ACCOUNT, ?], emitter=FluxSink(ERROR), uid=c0d0b865-d1cd-40de-a076-81364badfd9d)
```