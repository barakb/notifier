package com.totango.notifier.server

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.cloud.sleuth.instrument.kafka.TracingKafkaConsumerFactory
import org.springframework.cloud.sleuth.instrument.kafka.TracingKafkaProducerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions


@Configuration
class NotifierServerConfiguration {

    @Bean
    fun senderOptions(properties: NotifierServerProperties): SenderOptions<String, String> {
        val props: MutableMap<String, Any> = HashMap()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = properties.bootstrapServers
        props[ProducerConfig.CLIENT_ID_CONFIG] = "notifier-producer"
        props[ProducerConfig.ACKS_CONFIG] = "all"
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        return SenderOptions.create(props)
    }

    @Bean
    fun sender(tracingKafkaProducerFactory: TracingKafkaProducerFactory, senderOptions: SenderOptions<String, String>): KafkaSender<String, String> =
        KafkaSender.create(tracingKafkaProducerFactory, senderOptions)

    @Bean
    fun receiverOptions(properties: NotifierServerProperties): ReceiverOptions<String, String> {
        val props: MutableMap<String, Any> = HashMap()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = properties.bootstrapServers
        props[ConsumerConfig.CLIENT_ID_CONFIG] = "notifier-consumer"
        props[ConsumerConfig.GROUP_ID_CONFIG] = "notifier-group"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        return ReceiverOptions.create<String, String>(props).subscription(listOf(properties.topic))
    }

    @Bean
    fun receiver(tracingKafkaConsumerFactory: TracingKafkaConsumerFactory, receiverOptions: ReceiverOptions<String, String>): KafkaReceiver<String, String> =
        KafkaReceiver.create(tracingKafkaConsumerFactory, receiverOptions)

}