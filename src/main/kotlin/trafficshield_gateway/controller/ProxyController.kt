package trafficshield_gateway.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.ProxyResponse
import trafficshield_gateway.service.ProxyService

@Tag(
    name = "Proxy",
    description = "Routes client requests to registered backend service instances using load balancing, rate limiting, and circuit breaker checks"
)
@RestController
@RequestMapping("/proxy")
class ProxyController(
    private val proxyService: ProxyService
) {

    @Operation(
        summary = "Proxy a GET request to a registered backend service",
        description = """
        Routes the request to one of the registered backend service instances.
        
        Flow:
        1. Checks Redis-backed rate limit using client-id header
        2. Filters out instances with OPEN circuit breaker
        3. Selects a target instance using configured load balancing strategy
        4. Forwards the request
        5. Stores request metrics in PostgreSQL
    """
    )
    @GetMapping("/{serviceName}/**")
    fun proxyGet(
        @Parameter(description = "Registered service name, e.g. user-service or payment-service")
        @PathVariable serviceName: String,

        @Parameter(description = "Client identifier used for rate limiting")
        @RequestHeader(value = "client-id", defaultValue = "anonymous") clientId: String,

        request: HttpServletRequest
    ): ProxyResponse {
        val fullPath = request.requestURI
        val pathAfterServiceName = fullPath.substringAfter("/proxy/$serviceName/")

        return proxyService.forwardGetRequest(
            clientId = clientId,
            serviceName = serviceName,
            path = pathAfterServiceName
        )
    }
}