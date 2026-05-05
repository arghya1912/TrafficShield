package trafficshield_gateway.controller.admin

import trafficshield_gateway.repository.RequestMetricRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
}