package trafficshield_gateway.service

import org.springframework.stereotype.Service
import trafficshield_gateway.config.RateLimitConfig
import trafficshield_gateway.config.TrafficShieldProperties
import trafficshield_gateway.model.ServiceInstance

@Service
class ServiceRegistry(
    private val properties: TrafficShieldProperties
) {

    fun getAllServices(): Map<String, List<ServiceInstance>> {
        return properties.services.mapValues { it.value.instances }
    }

    fun getInstances(serviceName: String): List<ServiceInstance> {
        return properties.services[serviceName]?.instances
            ?: throw IllegalArgumentException("No service registered with name: $serviceName")
    }

    fun getRateLimitConfig(serviceName: String): RateLimitConfig {
        return properties.services[serviceName]?.rateLimit
            ?: throw IllegalArgumentException("No rate limit config found for service: $serviceName")
    }
}