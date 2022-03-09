package com.totango.notifier.server

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notifier.server.kafka")
class NotifierServerProperties {
    var bootstrapServers: String = "localhost:9092"
    var topic: String = "notifications"

}