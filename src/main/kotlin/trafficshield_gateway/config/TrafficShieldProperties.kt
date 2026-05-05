package trafficshield_gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import trafficshield_gateway.model.ServiceInstance

@Component
@ConfigurationProperties(prefix = "trafficshield")
class TrafficShieldProperties {
    var services: MutableMap<String, ServiceConfig> = mutableMapOf()
}

class ServiceConfig {
    var rateLimit: RateLimitConfig = RateLimitConfig()
    var instances: MutableList<ServiceInstance> = mutableListOf()
}

class RateLimitConfig {
    var capacity: Long = 5
    var refillRatePerSecond: Long = 1
}