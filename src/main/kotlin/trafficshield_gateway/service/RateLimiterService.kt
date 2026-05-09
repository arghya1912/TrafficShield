package trafficshield_gateway.service

import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service
import trafficshield_gateway.config.RateLimitConfig
import trafficshield_gateway.model.RateLimitStateResponse
import java.util.concurrent.ConcurrentHashMap

@Service
class RateLimiterService(
    private val redisTemplate: StringRedisTemplate
) {

    private val inMemoryBuckets = ConcurrentHashMap<String, InMemoryBucket>()

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

        return try {
            isAllowedUsingRedis(
                clientId = safeClientId,
                serviceName = serviceName,
                rateLimitConfig = rateLimitConfig
            )
        } catch (ex: RedisConnectionFailureException) {
            isAllowedUsingInMemoryFallback(
                clientId = safeClientId,
                serviceName = serviceName,
                rateLimitConfig = rateLimitConfig
            )
        } catch (ex: Exception) {
            isAllowedUsingInMemoryFallback(
                clientId = safeClientId,
                serviceName = serviceName,
                rateLimitConfig = rateLimitConfig
            )
        }
    }

    private fun isAllowedUsingRedis(
        clientId: String,
        serviceName: String,
        rateLimitConfig: RateLimitConfig
    ): Boolean {
        val tokensKey = tokensKey(clientId, serviceName)
        val timestampKey = timestampKey(clientId, serviceName)

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

    private fun isAllowedUsingInMemoryFallback(
        clientId: String,
        serviceName: String,
        rateLimitConfig: RateLimitConfig
    ): Boolean {
        val key = inMemoryKey(clientId, serviceName)
        val nowInSeconds = System.currentTimeMillis() / 1000

        val bucket = inMemoryBuckets.computeIfAbsent(key) {
            InMemoryBucket(
                tokens = rateLimitConfig.capacity.toDouble(),
                lastRefillEpochSecond = nowInSeconds
            )
        }

        synchronized(bucket) {
            val elapsedTime = maxOf(0, nowInSeconds - bucket.lastRefillEpochSecond)
            val tokensToAdd = elapsedTime * rateLimitConfig.refillRatePerSecond

            bucket.tokens = minOf(
                rateLimitConfig.capacity.toDouble(),
                bucket.tokens + tokensToAdd
            )
            bucket.lastRefillEpochSecond = nowInSeconds

            return if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0
                true
            } else {
                false
            }
        }
    }

    fun getRateLimitState(
        clientId: String,
        serviceName: String
    ): RateLimitStateResponse {
        val safeClientId = clientId.ifBlank { "anonymous" }

        return try {
            val tokensKey = tokensKey(safeClientId, serviceName)
            val timestampKey = timestampKey(safeClientId, serviceName)

            RateLimitStateResponse(
                clientId = safeClientId,
                serviceName = serviceName,
                tokensKey = tokensKey,
                timestampKey = timestampKey,
                currentTokens = redisTemplate.opsForValue().get(tokensKey),
                lastRefillTimestamp = redisTemplate.opsForValue().get(timestampKey)
            )
        } catch (ex: Exception) {
            val key = inMemoryKey(safeClientId, serviceName)
            val bucket = inMemoryBuckets[key]

            RateLimitStateResponse(
                clientId = safeClientId,
                serviceName = serviceName,
                tokensKey = "in-memory:$key:tokens",
                timestampKey = "in-memory:$key:timestamp",
                currentTokens = bucket?.tokens?.toString(),
                lastRefillTimestamp = bucket?.lastRefillEpochSecond?.toString()
            )
        }
    }

    fun resetRateLimit(
        clientId: String,
        serviceName: String
    ): RateLimitStateResponse {
        val safeClientId = clientId.ifBlank { "anonymous" }

        try {
            redisTemplate.delete(tokensKey(safeClientId, serviceName))
            redisTemplate.delete(timestampKey(safeClientId, serviceName))
        } catch (ex: Exception) {
            // Ignore Redis failure and reset in-memory fallback below.
        }

        inMemoryBuckets.remove(inMemoryKey(safeClientId, serviceName))

        return RateLimitStateResponse(
            clientId = safeClientId,
            serviceName = serviceName,
            tokensKey = tokensKey(safeClientId, serviceName),
            timestampKey = timestampKey(safeClientId, serviceName),
            currentTokens = null,
            lastRefillTimestamp = null
        )
    }

    private fun tokensKey(clientId: String, serviceName: String): String {
        return "rate_limit:$clientId:$serviceName:tokens"
    }

    private fun timestampKey(clientId: String, serviceName: String): String {
        return "rate_limit:$clientId:$serviceName:timestamp"
    }

    private fun inMemoryKey(clientId: String, serviceName: String): String {
        return "$clientId:$serviceName"
    }
}

private data class InMemoryBucket(
    var tokens: Double,
    var lastRefillEpochSecond: Long
)