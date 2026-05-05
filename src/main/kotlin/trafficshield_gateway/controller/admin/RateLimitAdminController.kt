package trafficshield_gateway.controller.admin

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.RateLimitStateResponse
import trafficshield_gateway.service.RateLimiterService

@RestController
@RequestMapping("/admin/rate-limits")
class RateLimitAdminController(
    private val rateLimiterService: RateLimiterService
) {

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