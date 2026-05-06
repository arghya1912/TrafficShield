package trafficshield_gateway.model

import java.time.Instant

enum class RequestOutcome {
    SUCCESS,
    RATE_LIMITED,
    DOWNSTREAM_FAILURE,
    CIRCUIT_OPEN,
    BAD_GATEWAY
}

data class RequestMetric(
    val requestId: String,
    val serviceName: String,
    val selectedInstance: String,
    val targetUrl: String,
    val statusCode: Int,
    val success: Boolean,
    val outcomeType: RequestOutcome,
    val latencyMs: Long,
    val errorMessage: String?,
    val createdAt: Instant = Instant.now()
)