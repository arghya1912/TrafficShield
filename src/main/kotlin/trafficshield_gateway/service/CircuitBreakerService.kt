package trafficshield_gateway.service

import org.springframework.stereotype.Service
import trafficshield_gateway.config.CircuitBreakerConfig
import trafficshield_gateway.model.CircuitBreakerSnapshot
import trafficshield_gateway.model.CircuitBreakerState
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class CircuitBreakerService {

    private val states = ConcurrentHashMap<String, CircuitBreakerInternalState>()

    fun isRequestAllowed(
        serviceName: String,
        instanceId: String,
        config: CircuitBreakerConfig
    ): Boolean {
        val key = key(serviceName, instanceId)
        val currentState = states.computeIfAbsent(key) { CircuitBreakerInternalState() }

        synchronized(currentState) {
            if (currentState.state == CircuitBreakerState.CLOSED) {
                return true
            }

            if (currentState.state == CircuitBreakerState.HALF_OPEN) {
                return true
            }

            val openedAt = currentState.openedAt ?: return true
            val secondsSinceOpened = Instant.now().epochSecond - openedAt.epochSecond

            return if (secondsSinceOpened >= config.openDurationSeconds) {
                currentState.state = CircuitBreakerState.HALF_OPEN
                true
            } else {
                false
            }
        }
    }

    fun recordSuccess(
        serviceName: String,
        instanceId: String
    ) {
        val key = key(serviceName, instanceId)
        val currentState = states.computeIfAbsent(key) { CircuitBreakerInternalState() }

        synchronized(currentState) {
            currentState.state = CircuitBreakerState.CLOSED
            currentState.failureCount = 0
            currentState.openedAt = null
            currentState.lastFailureReason = null
        }
    }

    fun recordFailure(
        serviceName: String,
        instanceId: String,
        config: CircuitBreakerConfig,
        reason: String?
    ) {
        val key = key(serviceName, instanceId)
        val currentState = states.computeIfAbsent(key) { CircuitBreakerInternalState() }

        synchronized(currentState) {
            currentState.failureCount += 1
            currentState.lastFailureReason = reason

            if (currentState.state == CircuitBreakerState.HALF_OPEN) {
                openCircuit(currentState)
                return
            }

            if (currentState.failureCount >= config.failureThreshold) {
                openCircuit(currentState)
            }
        }
    }

    fun getSnapshot(
        serviceName: String,
        instanceId: String,
        config: CircuitBreakerConfig
    ): CircuitBreakerSnapshot {
        val key = key(serviceName, instanceId)
        val currentState = states.computeIfAbsent(key) { CircuitBreakerInternalState() }

        synchronized(currentState) {
            return CircuitBreakerSnapshot(
                serviceName = serviceName,
                instanceId = instanceId,
                state = currentState.state,
                failureCount = currentState.failureCount,
                failureThreshold = config.failureThreshold,
                openDurationSeconds = config.openDurationSeconds,
                openedAt = currentState.openedAt,
                lastFailureReason = currentState.lastFailureReason
            )
        }
    }

    fun reset(
        serviceName: String,
        instanceId: String
    ) {
        val key = key(serviceName, instanceId)
        states[key] = CircuitBreakerInternalState()
    }

    fun resetAll() {
        states.clear()
    }

    private fun openCircuit(state: CircuitBreakerInternalState) {
        state.state = CircuitBreakerState.OPEN
        state.openedAt = Instant.now()
    }

    private fun key(serviceName: String, instanceId: String): String {
        return "$serviceName:$instanceId"
    }
}

private data class CircuitBreakerInternalState(
    var state: CircuitBreakerState = CircuitBreakerState.CLOSED,
    var failureCount: Int = 0,
    var openedAt: Instant? = null,
    var lastFailureReason: String? = null
)