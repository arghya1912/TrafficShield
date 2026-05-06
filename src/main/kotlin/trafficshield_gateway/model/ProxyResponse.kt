package trafficshield_gateway.model

data class ProxyResponse(
    val requestId: String,
    val serviceName: String,
    val selectedInstance: String,
    val targetUrl: String,
    val statusCode: Int,
    val outcome: RequestOutcome,
    val latencyMs: Long,
    val responseBody: Any?
)