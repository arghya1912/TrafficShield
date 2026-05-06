package trafficshield_gateway.controller.admin

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.TrafficSummaryResponse
import trafficshield_gateway.service.TrafficSummaryService

@RestController
@RequestMapping("/admin/ai")
class AiSummaryController(
    private val trafficSummaryService: TrafficSummaryService
) {

    @GetMapping("/traffic-summary")
    fun getTrafficSummary(): TrafficSummaryResponse {
        return trafficSummaryService.generateSummary()
    }
}