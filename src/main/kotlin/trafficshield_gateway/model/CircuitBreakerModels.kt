package trafficshield_gateway.model

import java.time.Instant

enum class CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}

data class CircuitBreakerSnapshot(
    val serviceName: String,
    val instanceId: String,
    val state: CircuitBreakerState,
    val failureCount: Int,
    val failureThreshold: Int,
    val openDurationSeconds: Long,
    val openedAt: Instant?,
    val lastFailureReason: String?
)