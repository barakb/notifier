[![build](https://github.com/barakb/notifier/actions/workflows/build.yml/badge.svg)](https://github.com/barakb/notifier/actions/workflows/build.yml)
[![Renovate enabled](https://img.shields.io/badge/renovate-enabled-brightgreen.svg)](https://renovatebot.com/)

### The goal of this project
To provide an easy way for sending and receiving notifications between processes

### The structure of this project
1. [A Notifier client spring boot starter](https://github.com/barakb/notifier/tree/master/notifier-client-starter) -- a spring boot started that when include in a spring boot project automatically create a Notifier client
configuration is done with one property `notifier.server.url` that should set the url (possible behind load balancer) of the Notifier server
for example `notifier.server.url=ws://localhost:6565/rsocket`
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
1. Waiting for the notification is written to the kafka topic
```kotlin
notifier.notify("foo")
```
3. Waiting only for the notification to be sent to the server
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
3. stand-alone subscription on the other hand is a bit heavier because each subscriber get its own Kafka reactive consumer, this subscriber gets events as fast as it consumes them but not faster (back-pressure is working).
```kotlin
val standAloneDisposable: Disposable = notifier.subscribe("FOO *", SubscriberMode.Standalone).doOnNext{
    logger.debug("standalone subscriber got $it")
}.subscribe()
```