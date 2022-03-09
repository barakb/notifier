package com.totango.notifier.client

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notifier.server.url")
class NotifierClientProperties {
     var url: String = "ws://localhost:6565/rsocket"
}
