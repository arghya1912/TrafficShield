package trafficshield_gateway.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import trafficshield_gateway.model.OutcomeMetricsResponse
import trafficshield_gateway.model.RequestMetric
import trafficshield_gateway.model.OverallMetricsResponse
import trafficshield_gateway.model.ServiceMetricsResponse
import java.sql.Timestamp

@Repository
class RequestMetricRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    fun save(metric: RequestMetric) {
        val sql = """
        INSERT INTO request_metrics (
            request_id,
            service_name,
            selected_instance,
            target_url,
            status_code,
            success,
            outcome_type,
            latency_ms,
            error_message,
            created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """.trimIndent()

        jdbcTemplate.update(
            sql,
            metric.requestId,
            metric.serviceName,
            metric.selectedInstance,
            metric.targetUrl,
            metric.statusCode,
            metric.success,
            metric.outcomeType.name,
            metric.latencyMs,
            metric.errorMessage,
            Timestamp.from(metric.createdAt)
        )
    }

    fun findRecent(limit: Int = 20): List<Map<String, Any>> {
        val sql = """
        SELECT request_id, service_name, selected_instance, target_url,
               status_code, success, outcome_type, latency_ms, error_message, created_at
        FROM request_metrics
        ORDER BY created_at DESC
        LIMIT ?
    """.trimIndent()

        return jdbcTemplate.queryForList(sql, limit)
    }

    fun getMetricsByOutcome(): List<OutcomeMetricsResponse> {
        val sql = """
        SELECT outcome_type, COUNT(*) AS total_requests
        FROM request_metrics
        GROUP BY outcome_type
        ORDER BY total_requests DESC
    """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            OutcomeMetricsResponse(
                outcomeType = rs.getString("outcome_type"),
                totalRequests = rs.getLong("total_requests")
            )
        }
    }

    fun getOverallMetrics(): OverallMetricsResponse {
        val sql = """
        SELECT
            COUNT(*) AS total_requests,
            COALESCE(SUM(CASE WHEN success = true THEN 1 ELSE 0 END), 0) AS successful_requests,
            COALESCE(SUM(CASE WHEN success = false THEN 1 ELSE 0 END), 0) AS failed_requests,
            COALESCE(AVG(latency_ms), 0) AS average_latency_ms,
            COALESCE(MAX(latency_ms), 0) AS max_latency_ms
        FROM request_metrics
    """.trimIndent()

        val row = jdbcTemplate.queryForMap(sql)

        val totalRequests = (row["total_requests"] as Number).toLong()
        val successfulRequests = (row["successful_requests"] as Number).toLong()
        val failedRequests = (row["failed_requests"] as Number).toLong()
        val averageLatencyMs = (row["average_latency_ms"] as Number).toDouble()
        val maxLatencyMs = (row["max_latency_ms"] as Number).toLong()

        val successRate = if (totalRequests == 0L) {
            0.0
        } else {
            (successfulRequests.toDouble() / totalRequests.toDouble()) * 100
        }

        return OverallMetricsResponse(
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            failedRequests = failedRequests,
            successRate = successRate,
            averageLatencyMs = averageLatencyMs,
            maxLatencyMs = maxLatencyMs
        )
    }

    fun getMetricsByService(): List<ServiceMetricsResponse> {
        val sql = """
        SELECT
            service_name,
            COUNT(*) AS total_requests,
            COALESCE(SUM(CASE WHEN success = true THEN 1 ELSE 0 END), 0) AS successful_requests,
            COALESCE(SUM(CASE WHEN success = false THEN 1 ELSE 0 END), 0) AS failed_requests,
            COALESCE(AVG(latency_ms), 0) AS average_latency_ms,
            COALESCE(MAX(latency_ms), 0) AS max_latency_ms
        FROM request_metrics
        GROUP BY service_name
        ORDER BY total_requests DESC
    """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            val totalRequests = rs.getLong("total_requests")
            val successfulRequests = rs.getLong("successful_requests")
            val failedRequests = rs.getLong("failed_requests")

            val successRate = if (totalRequests == 0L) {
                0.0
            } else {
                (successfulRequests.toDouble() / totalRequests.toDouble()) * 100
            }

            ServiceMetricsResponse(
                serviceName = rs.getString("service_name"),
                totalRequests = totalRequests,
                successfulRequests = successfulRequests,
                failedRequests = failedRequests,
                successRate = successRate,
                averageLatencyMs = rs.getDouble("average_latency_ms"),
                maxLatencyMs = rs.getLong("max_latency_ms")
            )
        }
    }

    fun getMetricsForService(serviceName: String): ServiceMetricsResponse {
        val sql = """
        SELECT
            service_name,
            COUNT(*) AS total_requests,
            COALESCE(SUM(CASE WHEN success = true THEN 1 ELSE 0 END), 0) AS successful_requests,
            COALESCE(SUM(CASE WHEN success = false THEN 1 ELSE 0 END), 0) AS failed_requests,
            COALESCE(AVG(latency_ms), 0) AS average_latency_ms,
            COALESCE(MAX(latency_ms), 0) AS max_latency_ms
        FROM request_metrics
        WHERE service_name = ?
        GROUP BY service_name
    """.trimIndent()

        return jdbcTemplate.query(sql, { rs, _ ->
            val totalRequests = rs.getLong("total_requests")
            val successfulRequests = rs.getLong("successful_requests")
            val failedRequests = rs.getLong("failed_requests")

            val successRate = if (totalRequests == 0L) {
                0.0
            } else {
                (successfulRequests.toDouble() / totalRequests.toDouble()) * 100
            }

            ServiceMetricsResponse(
                serviceName = rs.getString("service_name"),
                totalRequests = totalRequests,
                successfulRequests = successfulRequests,
                failedRequests = failedRequests,
                successRate = successRate,
                averageLatencyMs = rs.getDouble("average_latency_ms"),
                maxLatencyMs = rs.getLong("max_latency_ms")
            )
        }, serviceName).firstOrNull()
            ?: ServiceMetricsResponse(
                serviceName = serviceName,
                totalRequests = 0,
                successfulRequests = 0,
                failedRequests = 0,
                successRate = 0.0,
                averageLatencyMs = 0.0,
                maxLatencyMs = 0
            )
    }
    fun getTopProblematicService(): String? {
        val sql = """
        SELECT service_name
        FROM request_metrics
        WHERE success = false
        GROUP BY service_name
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            rs.getString("service_name")
        }.firstOrNull()
    }

    fun getDominantFailureType(): String? {
        val sql = """
        SELECT outcome_type
        FROM request_metrics
        WHERE success = false
        GROUP BY outcome_type
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            rs.getString("outcome_type")
        }.firstOrNull()
    }
}