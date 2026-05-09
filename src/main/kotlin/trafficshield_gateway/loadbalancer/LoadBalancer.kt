package trafficshield_gateway.loadbalancer

import trafficshield_gateway.model.LoadBalancingStrategy
import trafficshield_gateway.model.ServiceInstance

interface LoadBalancer {
    fun choose(
        serviceName: String,
        instances: List<ServiceInstance>,
        strategy: LoadBalancingStrategy
    ): ServiceInstance
}