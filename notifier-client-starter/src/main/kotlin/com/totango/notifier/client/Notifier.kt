package com.totango.notifier.client

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface Notifier {
    fun notify(notification: String) : Mono<Void>
    fun oneway(notification: String): Mono<Void>
    fun subscribe(pattern: String): Flux<Event>
}