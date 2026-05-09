package trafficshield_gateway.loadbalancer

import org.springframework.stereotype.Component
import trafficshield_gateway.model.LoadBalancingStrategy
import trafficshield_gateway.model.ServiceInstance
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class RoundRobinLoadBalancer : LoadBalancer {

    private val counters = ConcurrentHashMap<String, AtomicInteger>()

    override fun choose(
        serviceName: String,
        instances: List<ServiceInstance>,
        strategy: LoadBalancingStrategy
    ): ServiceInstance {
        if (instances.isEmpty()) {
            throw IllegalArgumentException("No instances available for service: $serviceName")
        }

        return when (strategy) {
            LoadBalancingStrategy.ROUND_ROBIN ->
                chooseRoundRobin(serviceName, instances)

            LoadBalancingStrategy.WEIGHTED_ROUND_ROBIN ->
                chooseWeightedRoundRobin(serviceName, instances)
        }
    }

    private fun chooseRoundRobin(
        serviceName: String,
        instances: List<ServiceInstance>
    ): ServiceInstance {
        val counter = counters.computeIfAbsent("rr:$serviceName") { AtomicInteger(0) }
        val index = Math.floorMod(counter.getAndIncrement(), instances.size)

        return instances[index]
    }

    private fun chooseWeightedRoundRobin(
        serviceName: String,
        instances: List<ServiceInstance>
    ): ServiceInstance {
        val weightedInstances = instances.flatMap { instance ->
            val safeWeight = if (instance.weight <= 0) 1 else instance.weight
            List(safeWeight) { instance }
        }

        val counter = counters.computeIfAbsent("wrr:$serviceName") { AtomicInteger(0) }
        val index = Math.floorMod(counter.getAndIncrement(), weightedInstances.size)

        return weightedInstances[index]
    }
}