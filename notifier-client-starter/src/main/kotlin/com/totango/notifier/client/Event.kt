package com.totango.notifier.client

data class Event(val message: String) {
    @Suppress("unused")
    val tokens: List<String>
        get() = message.split(Regex("\\s+"))
}