package com.totango.notifier.server

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.sleuth.instrument.kafka.TracingKafkaConsumerFactory
import org.springframework.cloud.sleuth.instrument.kafka.TracingKafkaProducerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import java.util.*
import kotlin.collections.HashMap


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
    fun receiverProps(properties: NotifierServerProperties): Map<String,Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = properties.bootstrapServers
        props[ConsumerConfig.CLIENT_ID_CONFIG] = "notifier-consumer"
        props[ConsumerConfig.GROUP_ID_CONFIG] = "notifier-group"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = true
        return props
    }

    @Bean
    @Qualifier("shared")
    fun receiverOptions(properties: NotifierServerProperties, receiverProps:Map<String,Any>): ReceiverOptions<String, String> {
        return ReceiverOptions.create<String, String>(receiverProps).subscription(listOf(properties.topic))
    }

    @Bean
    @Qualifier("shared")
    fun receiver(tracingKafkaConsumerFactory: TracingKafkaConsumerFactory, @Qualifier("shared") receiverOptions: ReceiverOptions<String, String>): KafkaReceiver<String, String> =
        KafkaReceiver.create(tracingKafkaConsumerFactory, receiverOptions)

    @Bean
    @Scope("prototype")
    fun standaloneReceiverOptions(properties: NotifierServerProperties, receiverProps:Map<String,Any>): ReceiverOptions<String, String> {
        val uid = UUID.randomUUID().toString()
        val standaloneProps = receiverProps +
                mapOf(
                    (ConsumerConfig.CLIENT_ID_CONFIG to "notifier-consumer stand alone $uid"),
                    (ConsumerConfig.GROUP_ID_CONFIG to "notifier-group stand alone $uid" )
                )
        return ReceiverOptions.create<String, String>(standaloneProps).subscription(listOf(properties.topic))
    }

    @Bean
    @Scope("prototype")
    fun standaloneReceiver(tracingKafkaConsumerFactory: TracingKafkaConsumerFactory, standaloneReceiverOptions: ReceiverOptions<String, String>): KafkaReceiver<String, String> =
        KafkaReceiver.create(tracingKafkaConsumerFactory, standaloneReceiverOptions)


}