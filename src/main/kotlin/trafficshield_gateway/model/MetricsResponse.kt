package trafficshield_gateway.model

data class OverallMetricsResponse(
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val successRate: Double,
    val averageLatencyMs: Double,
    val maxLatencyMs: Long
)

data class ServiceMetricsResponse(
    val serviceName: String,
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val successRate: Double,
    val averageLatencyMs: Double,
    val maxLatencyMs: Long
)