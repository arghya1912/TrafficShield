package trafficshield_gateway.model

data class TrafficSummaryResponse(
    val riskLevel: RiskLevel,
    val summary: String,
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val successRate: Double,
    val averageLatencyMs: Double,
    val topProblematicService: String?,
    val dominantFailureType: String?,
    val suggestions: List<String>
)

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}