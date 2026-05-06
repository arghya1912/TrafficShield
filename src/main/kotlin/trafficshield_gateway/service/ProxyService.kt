package trafficshield_gateway.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import trafficshield_gateway.loadbalancer.LoadBalancer
import trafficshield_gateway.model.ProxyResponse
import trafficshield_gateway.model.RequestMetric
import trafficshield_gateway.model.RequestOutcome
import trafficshield_gateway.repository.RequestMetricRepository
import java.util.UUID

@Service
class ProxyService(
    private val serviceRegistry: ServiceRegistry,
    private val loadBalancer: LoadBalancer,
    private val restTemplate: RestTemplate,
    private val requestMetricRepository: RequestMetricRepository,
    private val rateLimiterService: RateLimiterService,
    private val circuitBreakerService: CircuitBreakerService,
    private val objectMapper: ObjectMapper
) {

    fun forwardGetRequest(clientId: String, serviceName: String, path: String): ProxyResponse {
        val requestId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        val rateLimitConfig = serviceRegistry.getRateLimitConfig(serviceName)
        val allowed = rateLimiterService.isAllowed(
            clientId = clientId,
            serviceName = serviceName,
            rateLimitConfig = rateLimitConfig
        )

        if (!allowed) {
            val latencyMs = System.currentTimeMillis() - startTime

            requestMetricRepository.save(
                RequestMetric(
                    requestId = requestId,
                    serviceName = serviceName,
                    selectedInstance = "N/A",
                    targetUrl = "N/A",
                    statusCode = 429,
                    success = false,
                    outcomeType = RequestOutcome.RATE_LIMITED,
                    latencyMs = latencyMs,
                    errorMessage = "Rate limit exceeded for clientId=$clientId"
                )
            )

            return ProxyResponse(
                requestId = requestId,
                serviceName = serviceName,
                selectedInstance = "N/A",
                targetUrl = "N/A",
                statusCode = 429,
                outcome = RequestOutcome.RATE_LIMITED,
                latencyMs = latencyMs,
                responseBody = mapOf(
                    "message" to "Rate limit exceeded",
                    "clientId" to clientId,
                    "serviceName" to serviceName
                )
            )
        }

        val circuitBreakerConfig = serviceRegistry.getCircuitBreakerConfig(serviceName)
        val instances = serviceRegistry.getInstances(serviceName)

        val availableInstances = instances.filter { instance ->
            circuitBreakerService.isRequestAllowed(
                serviceName = serviceName,
                instanceId = instance.instanceId,
                config = circuitBreakerConfig
            )
        }

        if (availableInstances.isEmpty()) {
            val latencyMs = System.currentTimeMillis() - startTime

            requestMetricRepository.save(
                RequestMetric(
                    requestId = requestId,
                    serviceName = serviceName,
                    selectedInstance = "N/A",
                    targetUrl = "N/A",
                    statusCode = 503,
                    success = false,
                    outcomeType = RequestOutcome.CIRCUIT_OPEN,
                    latencyMs = latencyMs,
                    errorMessage = "All instances unavailable due to open circuit breakers"
                )
            )

            return ProxyResponse(
                requestId = requestId,
                serviceName = serviceName,
                selectedInstance = "N/A",
                targetUrl = "N/A",
                statusCode = 503,
                outcome = RequestOutcome.CIRCUIT_OPEN,
                latencyMs = latencyMs,
                responseBody = mapOf(
                    "message" to "All instances unavailable due to open circuit breakers",
                    "serviceName" to serviceName
                )
            )
        }

        val selectedInstance = loadBalancer.choose(serviceName, availableInstances)

        val normalizedPath = path.trimStart('/')
        val targetUrl = "${selectedInstance.baseUrl}/$normalizedPath"

        return try {
            val response = restTemplate.exchange(
                targetUrl,
                HttpMethod.GET,
                null,
                String::class.java
            )

            val latencyMs = System.currentTimeMillis() - startTime
            val statusCode = response.statusCode.value()

            val outcomeType = if (statusCode in 200..299) {
                RequestOutcome.SUCCESS
            } else {
                RequestOutcome.DOWNSTREAM_FAILURE
            }

            if (statusCode in 200..299) {
                circuitBreakerService.recordSuccess(
                    serviceName = serviceName,
                    instanceId = selectedInstance.instanceId
                )
            } else {
                circuitBreakerService.recordFailure(
                    serviceName = serviceName,
                    instanceId = selectedInstance.instanceId,
                    config = circuitBreakerConfig,
                    reason = "HTTP $statusCode"
                )
            }

            requestMetricRepository.save(
                RequestMetric(
                    requestId = requestId,
                    serviceName = serviceName,
                    selectedInstance = selectedInstance.instanceId,
                    targetUrl = targetUrl,
                    statusCode = statusCode,
                    success = statusCode in 200..299,
                    outcomeType = outcomeType,
                    latencyMs = latencyMs,
                    errorMessage = null
                )
            )

            ProxyResponse(
                requestId = requestId,
                serviceName = serviceName,
                selectedInstance = selectedInstance.instanceId,
                targetUrl = targetUrl,
                statusCode = statusCode,
                outcome = outcomeType,
                latencyMs = latencyMs,
                responseBody = parseResponseBody(response.body)
            )
        } catch (ex: HttpStatusCodeException) {
            val latencyMs = System.currentTimeMillis() - startTime
            val statusCode = ex.statusCode.value()

            circuitBreakerService.recordFailure(
                serviceName = serviceName,
                instanceId = selectedInstance.instanceId,
                config = circuitBreakerConfig,
                reason = "HTTP ${ex.statusCode.value()}"
            )

            requestMetricRepository.save(
                RequestMetric(
                    requestId = requestId,
                    serviceName = serviceName,
                    selectedInstance = selectedInstance.instanceId,
                    targetUrl = targetUrl,
                    statusCode = statusCode,
                    success = false,
                    outcomeType = RequestOutcome.DOWNSTREAM_FAILURE,
                    latencyMs = latencyMs,
                    errorMessage = ex.responseBodyAsString
                )
            )

            ProxyResponse(
                requestId = requestId,
                serviceName = serviceName,
                selectedInstance = selectedInstance.instanceId,
                targetUrl = targetUrl,
                statusCode = statusCode,
                outcome = RequestOutcome.DOWNSTREAM_FAILURE,
                latencyMs = latencyMs,
                responseBody = parseResponseBody(ex.responseBodyAsString)
            )
        } catch (ex: Exception) {
            val latencyMs = System.currentTimeMillis() - startTime

            circuitBreakerService.recordFailure(
                serviceName = serviceName,
                instanceId = selectedInstance.instanceId,
                config = circuitBreakerConfig,
                reason = ex.message
            )

            requestMetricRepository.save(
                RequestMetric(
                    requestId = requestId,
                    serviceName = serviceName,
                    selectedInstance = selectedInstance.instanceId,
                    targetUrl = targetUrl,
                    statusCode = 502,
                    success = false,
                    outcomeType = RequestOutcome.BAD_GATEWAY,
                    latencyMs = latencyMs,
                    errorMessage = ex.message
                )
            )

            ProxyResponse(
                requestId = requestId,
                serviceName = serviceName,
                selectedInstance = selectedInstance.instanceId,
                targetUrl = targetUrl,
                statusCode = 502,
                outcome = RequestOutcome.BAD_GATEWAY,
                latencyMs = latencyMs,
                responseBody = mapOf(
                    "message" to (ex.message ?: "Bad Gateway")
                )
            )
        }
    }

    private fun parseResponseBody(responseBody: String?): Any? {
        if (responseBody.isNullOrBlank()) {
            return null
        }

        return try {
            objectMapper.readValue(responseBody, Map::class.java)
        } catch (ex: Exception) {
            responseBody
        }
    }
}