package trafficshield_gateway.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.TrafficSummaryResponse
import trafficshield_gateway.service.TrafficSummaryService

@Tag(
    name = "AI Summary",
    description = "AI-style traffic and incident summary generated from gateway metrics"
)
@RestController
@RequestMapping("/admin/ai")
class AiSummaryController(
    private val trafficSummaryService: TrafficSummaryService
) {

    @Operation(
        summary = "Generate AI-style traffic summary",
        description = """
        Generates a rule-based incident summary from gateway metrics.
        
        It identifies:
        - Risk level
        - Top problematic service
        - Dominant failure type
        - Success rate
        - Suggested remediation steps
        
        This is currently deterministic and does not require a paid AI API key.
    """
    )
    @GetMapping("/traffic-summary")
    fun getTrafficSummary(): TrafficSummaryResponse {
        return trafficSummaryService.generateSummary()
    }
}