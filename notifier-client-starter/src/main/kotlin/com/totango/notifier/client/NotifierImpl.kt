package com.totango.notifier.client

import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveFlux
import org.springframework.messaging.rsocket.retrieveMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


open class NotifierImpl(private val requester: RSocketRequester) : Notifier {

    override fun notify(notification: String): Mono<Void> =
        requester.route("notify")
            .data(notification.normalize())
            .retrieveMono()


    override fun oneway(notification: String): Mono<Void> =
        requester.route("oneway")
            .data(notification.normalize())
            .send()



    override fun subscribe(pattern: String): Flux<Event> =
        requester.route("subscribe")
            .data(pattern.normalize())
            .retrieveFlux<String>()
            .map { e -> Event(e) }


    private fun String.normalize(): String = this.uppercase().trim().replace("\\s+", " ")

}