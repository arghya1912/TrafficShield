package trafficshield_gateway.service

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service
import trafficshield_gateway.config.RateLimitConfig
import trafficshield_gateway.model.RateLimitStateResponse

@Service
class RateLimiterService(
    private val redisTemplate: StringRedisTemplate
) {

    private val tokenBucketScript = DefaultRedisScript<Long>().apply {
        setScriptText(
            """
            local tokens_key = KEYS[1]
            local timestamp_key = KEYS[2]

            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local requested_tokens = tonumber(ARGV[4])
            local ttl_seconds = tonumber(ARGV[5])

            local current_tokens = tonumber(redis.call("get", tokens_key))
            if current_tokens == nil then
                current_tokens = capacity
            end

            local last_refill_time = tonumber(redis.call("get", timestamp_key))
            if last_refill_time == nil then
                last_refill_time = now
            end

            local elapsed_time = math.max(0, now - last_refill_time)
            local tokens_to_add = elapsed_time * refill_rate
            local updated_tokens = math.min(capacity, current_tokens + tokens_to_add)

            local allowed = 0
            if updated_tokens >= requested_tokens then
                allowed = 1
                updated_tokens = updated_tokens - requested_tokens
            end

            redis.call("setex", tokens_key, ttl_seconds, updated_tokens)
            redis.call("setex", timestamp_key, ttl_seconds, now)

            return allowed
            """.trimIndent()
        )
        resultType = Long::class.java
    }

    fun isAllowed(
        clientId: String,
        serviceName: String,
        rateLimitConfig: RateLimitConfig
    ): Boolean {
        val safeClientId = clientId.ifBlank { "anonymous" }

        val tokensKey = "rate_limit:$safeClientId:$serviceName:tokens"
        val timestampKey = "rate_limit:$safeClientId:$serviceName:timestamp"

        val nowInSeconds = System.currentTimeMillis() / 1000
        val requestedTokens = 1L
        val ttlSeconds = 3600L

        val result = redisTemplate.execute(
            tokenBucketScript,
            listOf(tokensKey, timestampKey),
            rateLimitConfig.capacity.toString(),
            rateLimitConfig.refillRatePerSecond.toString(),
            nowInSeconds.toString(),
            requestedTokens.toString(),
            ttlSeconds.toString()
        )

        return result == 1L
    }

    fun getRateLimitState(
        clientId: String,
        serviceName: String
    ): RateLimitStateResponse {
        val safeClientId = clientId.ifBlank { "anonymous" }

        val tokensKey = "rate_limit:$safeClientId:$serviceName:tokens"
        val timestampKey = "rate_limit:$safeClientId:$serviceName:timestamp"

        val currentTokens = redisTemplate.opsForValue().get(tokensKey)
        val lastRefillTimestamp = redisTemplate.opsForValue().get(timestampKey)

        return RateLimitStateResponse(
            clientId = safeClientId,
            serviceName = serviceName,
            tokensKey = tokensKey,
            timestampKey = timestampKey,
            currentTokens = currentTokens,
            lastRefillTimestamp = lastRefillTimestamp
        )
    }

    fun resetRateLimit(
        clientId: String,
        serviceName: String
    ): RateLimitStateResponse {
        val safeClientId = clientId.ifBlank { "anonymous" }

        val tokensKey = "rate_limit:$safeClientId:$serviceName:tokens"
        val timestampKey = "rate_limit:$safeClientId:$serviceName:timestamp"

        redisTemplate.delete(tokensKey)
        redisTemplate.delete(timestampKey)

        return RateLimitStateResponse(
            clientId = safeClientId,
            serviceName = serviceName,
            tokensKey = tokensKey,
            timestampKey = timestampKey,
            currentTokens = null,
            lastRefillTimestamp = null
        )
    }
}