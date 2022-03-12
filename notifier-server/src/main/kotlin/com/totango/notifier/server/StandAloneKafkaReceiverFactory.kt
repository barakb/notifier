package com.totango.notifier.server

import org.springframework.beans.factory.BeanFactory
import org.springframework.stereotype.Component
import reactor.kafka.receiver.KafkaReceiver

@Component
class StandaloneKafkaReceiverFactory(private val beanFactory: BeanFactory) {
    @Suppress("UNCHECKED_CAST")
    fun create() : KafkaReceiver<String, String> =
        beanFactory.getBean("standaloneReceiver", KafkaReceiver::class.java) as KafkaReceiver<String, String>
}