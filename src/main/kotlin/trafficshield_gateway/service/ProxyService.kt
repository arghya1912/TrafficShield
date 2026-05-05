package trafficshield_gateway.service

import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import trafficshield_gateway.loadbalancer.LoadBalancer
import trafficshield_gateway.model.ProxyResponse
import trafficshield_gateway.model.RequestMetric
import trafficshield_gateway.repository.RequestMetricRepository
import java.util.UUID

@Service
class ProxyService(
    private val serviceRegistry: ServiceRegistry,
    private val loadBalancer: LoadBalancer,
    private val restTemplate: RestTemplate,
    private val requestMetricRepository: RequestMetricRepository,
    private val rateLimiterService: RateLimiterService,
    private val circuitBreakerService: CircuitBreakerService
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
                    latencyMs = latencyMs,
                    errorMessage = "Rate limit exceeded for clientId=$clientId"
                )
            )

            return ProxyResponse(
                serviceName = serviceName,
                selectedInstance = "N/A",
                targetUrl = "N/A",
                statusCode = 429,
                responseBody = "Rate limit exceeded for clientId=$clientId"
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
                    latencyMs = latencyMs,
                    errorMessage = "All instances unavailable due to open circuit breakers"
                )
            )

            return ProxyResponse(
                serviceName = serviceName,
                selectedInstance = "N/A",
                targetUrl = "N/A",
                statusCode = 503,
                responseBody = "All instances unavailable due to open circuit breakers"
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
                    latencyMs = latencyMs,
                    errorMessage = null
                )
            )

            ProxyResponse(
                serviceName = serviceName,
                selectedInstance = selectedInstance.instanceId,
                targetUrl = targetUrl,
                statusCode = statusCode,
                responseBody = response.body ?: ""
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
                    latencyMs = latencyMs,
                    errorMessage = ex.responseBodyAsString
                )
            )

            ProxyResponse(
                serviceName = serviceName,
                selectedInstance = selectedInstance.instanceId,
                targetUrl = targetUrl,
                statusCode = statusCode,
                responseBody = ex.responseBodyAsString
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
                    latencyMs = latencyMs,
                    errorMessage = ex.message
                )
            )

            ProxyResponse(
                serviceName = serviceName,
                selectedInstance = selectedInstance.instanceId,
                targetUrl = targetUrl,
                statusCode = 502,
                responseBody = ex.message ?: "Bad Gateway"
            )
        }
    }
}