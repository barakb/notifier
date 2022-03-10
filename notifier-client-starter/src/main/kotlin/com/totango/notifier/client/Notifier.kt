package com.totango.notifier.client

import org.springframework.cloud.sleuth.annotation.NewSpan
import org.springframework.cloud.sleuth.annotation.SpanTag
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface Notifier {
    @NewSpan
    fun notify(@SpanTag("notification") notification: String) : Mono<Void>
    @NewSpan
    fun oneway(@SpanTag("notification") notification: String): Mono<Void>
    @NewSpan
    fun subscribe(@SpanTag("pattern") pattern: String): Flux<Event>
}