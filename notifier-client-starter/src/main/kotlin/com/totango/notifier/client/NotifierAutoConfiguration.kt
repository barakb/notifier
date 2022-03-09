package com.totango.notifier.client

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.util.MimeTypeUtils
import java.net.URI


@Configuration
@EnableConfigurationProperties(NotifierClientProperties::class)
class NotifierAutoConfiguration {


//    @Bean
//    @Scope("prototype")
//    @ConditionalOnProperty(value = ["spring.sleuth.rsocket.enabled"], matchIfMissing = true)
//    @ConditionalOnMissingBean(type = ["TraceRSocketAutoConfiguration"])
//    fun rSocketRequesterBuilder(): RSocketRequester.Builder =
//         RSocketRequester.builder()


    @Bean
    @ConditionalOnMissingBean
    fun requester(
        builder: RSocketRequester.Builder,
        notifierClientProperties: NotifierClientProperties
    ): RSocketRequester =
        builder
            .dataMimeType(MimeTypeUtils.APPLICATION_JSON)
            .websocket(URI.create(notifierClientProperties.url))

    @Bean
    @ConditionalOnMissingBean
    fun notifier(requester: RSocketRequester): Notifier {
        return NotifierImpl(requester)
    }

}