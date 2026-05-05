package trafficshield_gateway.controller.admin

import trafficshield_gateway.repository.RequestMetricRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.OverallMetricsResponse
import trafficshield_gateway.model.ServiceMetricsResponse

@RestController
@RequestMapping("/admin/metrics")
class MetricsController(
    private val requestMetricRepository: RequestMetricRepository
) {

    @GetMapping("/recent")
    fun getRecentMetrics(
        @RequestParam(defaultValue = "20") limit: Int
    ): List<Map<String, Any>> {
        return requestMetricRepository.findRecent(limit)
    }

    @GetMapping("/summary")
    fun getOverallMetrics(): OverallMetricsResponse {
        return requestMetricRepository.getOverallMetrics()
    }

    @GetMapping("/services")
    fun getMetricsByService(): List<ServiceMetricsResponse> {
        return requestMetricRepository.getMetricsByService()
    }

    @GetMapping("/services/{serviceName}")
    fun getMetricsForService(
        @PathVariable serviceName: String
    ): ServiceMetricsResponse {
        return requestMetricRepository.getMetricsForService(serviceName)
    }
}