package trafficshield_gateway.model

import java.time.Instant

data class MockServiceResponse(
    val serviceName: String,
    val instanceId: String,
    val status: String,
    val message: String,
    val timestamp: String = Instant.now().toString()
)