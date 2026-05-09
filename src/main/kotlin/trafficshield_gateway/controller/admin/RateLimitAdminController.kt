package trafficshield_gateway.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.RateLimitStateResponse
import trafficshield_gateway.service.RateLimiterService

@Tag(
    name = "Rate Limiter",
    description = "Inspect and reset Redis-backed token bucket rate-limit state"
)
@RestController
@RequestMapping("/admin/rate-limits")
class RateLimitAdminController(
    private val rateLimiterService: RateLimiterService
) {

    @Operation(
        summary = "Get rate-limit state",
        description = "Returns Redis keys and current token bucket state for a client-service pair."
    )
    @GetMapping("/{clientId}/{serviceName}")
    fun getRateLimitState(
        @PathVariable clientId: String,
        @PathVariable serviceName: String
    ): RateLimitStateResponse {
        return rateLimiterService.getRateLimitState(
            clientId = clientId,
            serviceName = serviceName
        )
    }

    @Operation(
        summary = "Reset rate-limit state",
        description = "Deletes Redis token bucket keys for a client-service pair."
    )
    @DeleteMapping("/{clientId}/{serviceName}")
    fun resetRateLimit(
        @PathVariable clientId: String,
        @PathVariable serviceName: String
    ): RateLimitStateResponse {
        return rateLimiterService.resetRateLimit(
            clientId = clientId,
            serviceName = serviceName
        )
    }
}