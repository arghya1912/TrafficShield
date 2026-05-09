package trafficshield_gateway.model

data class DemoResetResponse(
    val message: String,
    val clientId: String,
    val metricsCleared: Boolean,
    val circuitBreakersReset: Boolean,
    val rateLimitsResetForServices: List<String>
)

data class DemoRunResponse(
    val scenario: String,
    val clientId: String,
    val totalProxyCalls: Int,
    val results: List<ProxyResponse>,
    val nextSteps: Map<String, String>
)