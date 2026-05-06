package trafficshield_gateway.service

import org.springframework.stereotype.Service
import trafficshield_gateway.model.RiskLevel
import trafficshield_gateway.model.TrafficSummaryResponse
import trafficshield_gateway.repository.RequestMetricRepository

@Service
class TrafficSummaryService(
    private val requestMetricRepository: RequestMetricRepository
) {

    fun generateSummary(): TrafficSummaryResponse {
        val overallMetrics = requestMetricRepository.getOverallMetrics()
        val topProblematicService = requestMetricRepository.getTopProblematicService()
        val dominantFailureType = requestMetricRepository.getDominantFailureType()

        val riskLevel = calculateRiskLevel(
            successRate = overallMetrics.successRate,
            averageLatencyMs = overallMetrics.averageLatencyMs,
            failedRequests = overallMetrics.failedRequests
        )

        val summary = buildSummary(
            riskLevel = riskLevel,
            topProblematicService = topProblematicService,
            dominantFailureType = dominantFailureType,
            totalRequests = overallMetrics.totalRequests,
            failedRequests = overallMetrics.failedRequests,
            successRate = overallMetrics.successRate
        )

        val suggestions = buildSuggestions(
            riskLevel = riskLevel,
            dominantFailureType = dominantFailureType
        )

        return TrafficSummaryResponse(
            riskLevel = riskLevel,
            summary = summary,
            totalRequests = overallMetrics.totalRequests,
            successfulRequests = overallMetrics.successfulRequests,
            failedRequests = overallMetrics.failedRequests,
            successRate = overallMetrics.successRate,
            averageLatencyMs = overallMetrics.averageLatencyMs,
            topProblematicService = topProblematicService,
            dominantFailureType = dominantFailureType,
            suggestions = suggestions
        )
    }

    private fun calculateRiskLevel(
        successRate: Double,
        averageLatencyMs: Double,
        failedRequests: Long
    ): RiskLevel {
        return when {
            failedRequests == 0L -> RiskLevel.LOW
            successRate < 70.0 -> RiskLevel.HIGH
            successRate < 90.0 -> RiskLevel.MEDIUM
            averageLatencyMs > 1000 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    private fun buildSummary(
        riskLevel: RiskLevel,
        topProblematicService: String?,
        dominantFailureType: String?,
        totalRequests: Long,
        failedRequests: Long,
        successRate: Double
    ): String {
        if (totalRequests == 0L) {
            return "No traffic has been recorded yet. Send requests through the proxy endpoint to generate traffic insights."
        }

        if (failedRequests == 0L) {
            return "Traffic is healthy. All recorded requests completed successfully with no observed failures."
        }

        val serviceText = topProblematicService ?: "one or more services"
        val failureText = dominantFailureType ?: "unknown failure type"

        return when (riskLevel) {
            RiskLevel.HIGH ->
                "$serviceText is showing significant reliability issues. Out of $totalRequests requests, $failedRequests failed, with a success rate of ${"%.2f".format(successRate)}%. The dominant failure type is $failureText."

            RiskLevel.MEDIUM ->
                "$serviceText is showing moderate instability. Some requests are failing, with $failureText being the most common failure category."

            RiskLevel.LOW ->
                "Traffic is mostly healthy, but a small number of failures were observed. The most common failure category is $failureText."
        }
    }

    private fun buildSuggestions(
        riskLevel: RiskLevel,
        dominantFailureType: String?
    ): List<String> {
        val suggestions = mutableListOf<String>()

        when (dominantFailureType) {
            "RATE_LIMITED" -> {
                suggestions.add("Review rate-limit configuration for high-traffic clients.")
                suggestions.add("Check if the client is sending burst traffic beyond configured token capacity.")
            }

            "DOWNSTREAM_FAILURE" -> {
                suggestions.add("Inspect failing backend service instances and their recent error responses.")
                suggestions.add("Review circuit breaker state to confirm whether unstable instances are being isolated.")
            }

            "CIRCUIT_OPEN" -> {
                suggestions.add("Check circuit breaker snapshots to identify unavailable service instances.")
                suggestions.add("Reset circuit breakers only after confirming the backend instance is healthy.")
            }

            "BAD_GATEWAY" -> {
                suggestions.add("Verify backend service URLs and connectivity from the gateway.")
                suggestions.add("Check whether target services are down or misconfigured.")
            }

            else -> {
                suggestions.add("Review recent request metrics and service-level failure patterns.")
            }
        }

        if (riskLevel == RiskLevel.HIGH) {
            suggestions.add("Temporarily reduce traffic to unstable services and inspect logs for repeated failures.")
        }

        return suggestions
    }
}