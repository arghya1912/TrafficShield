package trafficshield_gateway.controller.admin

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.CircuitBreakerSnapshot
import trafficshield_gateway.service.CircuitBreakerService
import trafficshield_gateway.service.ServiceRegistry

@RestController
@RequestMapping("/admin/circuit-breakers")
class CircuitBreakerAdminController(
    private val circuitBreakerService: CircuitBreakerService,
    private val serviceRegistry: ServiceRegistry
) {

    @GetMapping
    fun getAllCircuitBreakers(): List<CircuitBreakerSnapshot> {
        return serviceRegistry.getAllServices().flatMap { (serviceName, instances) ->
            val config = serviceRegistry.getCircuitBreakerConfig(serviceName)

            instances.map { instance ->
                circuitBreakerService.getSnapshot(
                    serviceName = serviceName,
                    instanceId = instance.instanceId,
                    config = config
                )
            }
        }
    }

    @GetMapping("/{serviceName}/{instanceId}")
    fun getCircuitBreaker(
        @PathVariable serviceName: String,
        @PathVariable instanceId: String
    ): CircuitBreakerSnapshot {
        val config = serviceRegistry.getCircuitBreakerConfig(serviceName)

        return circuitBreakerService.getSnapshot(
            serviceName = serviceName,
            instanceId = instanceId,
            config = config
        )
    }

    @PostMapping("/{serviceName}/{instanceId}/reset")
    fun resetCircuitBreaker(
        @PathVariable serviceName: String,
        @PathVariable instanceId: String
    ): CircuitBreakerSnapshot {
        circuitBreakerService.reset(
            serviceName = serviceName,
            instanceId = instanceId
        )

        val config = serviceRegistry.getCircuitBreakerConfig(serviceName)

        return circuitBreakerService.getSnapshot(
            serviceName = serviceName,
            instanceId = instanceId,
            config = config
        )
    }
}