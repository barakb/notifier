package com.totango.notifier.server

import reactor.core.publisher.FluxSink
import java.util.UUID

data class Subscription(val pattern: List<String>, val emitter: FluxSink<String>, val uid: String = UUID.randomUUID().toString())