package trafficshield_gateway.model

data class RateLimitStateResponse(
    val clientId: String,
    val serviceName: String,
    val tokensKey: String,
    val timestampKey: String,
    val currentTokens: String?,
    val lastRefillTimestamp: String?
)