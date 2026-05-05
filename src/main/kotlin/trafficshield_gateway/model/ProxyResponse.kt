package trafficshield_gateway.model

data class ProxyResponse(
    val serviceName: String,
    val selectedInstance: String,
    val targetUrl: String,
    val statusCode: Int,
    val responseBody: String
)