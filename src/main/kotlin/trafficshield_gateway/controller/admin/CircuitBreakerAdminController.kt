package trafficshield_gateway.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.CircuitBreakerSnapshot
import trafficshield_gateway.service.CircuitBreakerService
import trafficshield_gateway.service.ServiceRegistry

@Tag(
    name = "Circuit Breaker",
    description = "Inspect and reset circuit breaker state for backend service instances"
)
@RestController
@RequestMapping("/admin/circuit-breakers")
class CircuitBreakerAdminController(
    private val circuitBreakerService: CircuitBreakerService,
    private val serviceRegistry: ServiceRegistry
) {

    @Operation(
        summary = "Get all circuit breaker states",
        description = "Returns circuit breaker state for every registered service instance."
    )
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

    @Operation(
        summary = "Get circuit breaker state for one instance",
        description = "Returns circuit breaker state, failure count, threshold, opened timestamp, and last failure reason."
    )
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

    @Operation(
        summary = "Reset circuit breaker for one instance",
        description = "Resets the selected circuit breaker back to CLOSED state."
    )
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