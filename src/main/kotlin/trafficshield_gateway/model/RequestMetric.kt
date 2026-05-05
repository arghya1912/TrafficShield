package trafficshield_gateway.model

import java.time.Instant

data class RequestMetric(
    val requestId: String,
    val serviceName: String,
    val selectedInstance: String,
    val targetUrl: String,
    val statusCode: Int,
    val success: Boolean,
    val latencyMs: Long,
    val errorMessage: String?,
    val createdAt:  Instant = Instant.now()
)