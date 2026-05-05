package trafficshield_gateway.loadbalancer

import org.springframework.stereotype.Component
import trafficshield_gateway.model.ServiceInstance
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class RoundRobinLoadBalancer : LoadBalancer {

    private val counters = ConcurrentHashMap<String, AtomicInteger>()

    override fun choose(serviceName: String, instances: List<ServiceInstance>): ServiceInstance {
        if (instances.isEmpty()) {
            throw IllegalArgumentException("No instances available for service: $serviceName")
        }

        val counter = counters.computeIfAbsent(serviceName) { AtomicInteger(0) }
        val index = Math.floorMod(counter.getAndIncrement(), instances.size)

        return instances[index]
    }
}