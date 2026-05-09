package trafficshield_gateway.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import trafficshield_gateway.repository.RequestMetricRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.OutcomeMetricsResponse
import trafficshield_gateway.model.OverallMetricsResponse
import trafficshield_gateway.model.ServiceMetricsResponse

@Tag(
    name = "Metrics",
    description = "Request metrics, service-level summaries, and outcome-based gateway statistics"
)
@RestController
@RequestMapping("/admin/metrics")
class MetricsController(
    private val requestMetricRepository: RequestMetricRepository
) {

    @Operation(
        summary = "Get recent request metrics",
        description = "Returns recently proxied requests with selected instance, status code, latency, outcome type, and error details."
    )
    @GetMapping("/recent")
    fun getRecentMetrics(
        @RequestParam(defaultValue = "20") limit: Int
    ): List<Map<String, Any>> {
        return requestMetricRepository.findRecent(limit)
    }

    @Operation(
        summary = "Get overall traffic summary",
        description = "Returns total requests, successful requests, failed requests, success rate, average latency, and max latency."
    )
    @GetMapping("/summary")
    fun getOverallMetrics(): OverallMetricsResponse {
        return requestMetricRepository.getOverallMetrics()
    }

    @Operation(
        summary = "Get service-wise metrics",
        description = "Returns request count, failure count, success rate, and latency metrics grouped by service."
    )
    @GetMapping("/services")
    fun getMetricsByService(): List<ServiceMetricsResponse> {
        return requestMetricRepository.getMetricsByService()
    }

    @Operation(
        summary = "Get metrics for one service",
        description = "Returns metrics for a specific registered service."
    )
    @GetMapping("/services/{serviceName}")
    fun getMetricsForService(
        @PathVariable serviceName: String
    ): ServiceMetricsResponse {
        return requestMetricRepository.getMetricsForService(serviceName)
    }

    @Operation(
        summary = "Get metrics grouped by outcome type",
        description = "Returns counts for SUCCESS, RATE_LIMITED, DOWNSTREAM_FAILURE, CIRCUIT_OPEN, and BAD_GATEWAY outcomes."
    )
    @GetMapping("/outcomes")
    fun getMetricsByOutcome(): List<OutcomeMetricsResponse> {
        return requestMetricRepository.getMetricsByOutcome()
    }
}