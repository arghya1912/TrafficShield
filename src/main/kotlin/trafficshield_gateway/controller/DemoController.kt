package trafficshield_gateway.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.DemoResetResponse
import trafficshield_gateway.model.DemoRunResponse
import trafficshield_gateway.model.ProxyResponse
import trafficshield_gateway.repository.RequestMetricRepository
import trafficshield_gateway.service.CircuitBreakerService
import trafficshield_gateway.service.ProxyService
import trafficshield_gateway.service.RateLimiterService
import trafficshield_gateway.service.ServiceRegistry

@Tag(
    name = "Demo",
    description = "Demo endpoints to reset project state and generate sample gateway traffic"
)
@RestController
@RequestMapping("/demo")
class DemoController(
    private val proxyService: ProxyService,
    private val requestMetricRepository: RequestMetricRepository,
    private val rateLimiterService: RateLimiterService,
    private val circuitBreakerService: CircuitBreakerService,
    private val serviceRegistry: ServiceRegistry
) {

    @Operation(
        summary = "Reset demo state",
        description = """
            Clears request metrics, resets all circuit breakers, and clears rate-limit state for the provided client.
            Use this before running a fresh demo scenario from Swagger.
        """
    )
    @PostMapping("/reset")
    fun resetDemo(
        @Parameter(description = "Client ID whose rate limit state should be reset")
        @RequestParam(defaultValue = "demo-user") clientId: String
    ): DemoResetResponse {
        val serviceNames = serviceRegistry.getAllServices().keys.toList()

        serviceNames.forEach { serviceName ->
            rateLimiterService.resetRateLimit(
                clientId = clientId,
                serviceName = serviceName
            )
        }

        circuitBreakerService.resetAll()
        requestMetricRepository.deleteAll()

        return DemoResetResponse(
            message = "Demo state reset successfully",
            clientId = clientId,
            metricsCleared = true,
            circuitBreakersReset = true,
            rateLimitsResetForServices = serviceNames
        )
    }

    @Operation(
        summary = "Run basic TrafficShield demo",
        description = """
            Generates sample traffic through the gateway.

            The scenario includes:
            - Successful user-service requests
            - Weighted routing to payment-service instances
            - Simulated downstream failures
            - Metrics generation
            - Circuit breaker impact
        """
    )
    @PostMapping("/run/basic")
    fun runBasicDemo(
        @Parameter(description = "Client ID used while generating demo traffic")
        @RequestParam(defaultValue = "demo-user") clientId: String
    ): DemoRunResponse {
        val results = mutableListOf<ProxyResponse>()

        repeat(4) {
            results.add(
                proxyService.forwardGetRequest(
                    clientId = clientId,
                    serviceName = "user-service",
                    path = "api/users/101"
                )
            )
        }

        repeat(8) {
            results.add(
                proxyService.forwardGetRequest(
                    clientId = clientId,
                    serviceName = "payment-service",
                    path = "api/payments/501"
                )
            )
        }

        return DemoRunResponse(
            scenario = "Basic TrafficShield demo: user-service success traffic + payment-service mixed success/failure traffic",
            clientId = clientId,
            totalProxyCalls = results.size,
            results = results,
            nextSteps = mapOf(
                "recentMetrics" to "/admin/metrics/recent",
                "summary" to "/admin/metrics/summary",
                "outcomes" to "/admin/metrics/outcomes",
                "serviceStatus" to "/admin/services/status",
                "circuitBreakers" to "/admin/circuit-breakers",
                "aiSummary" to "/admin/ai/traffic-summary"
            )
        )
    }
}