package trafficshield_gateway.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import trafficshield_gateway.model.RequestMetric
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
                latency_ms,
                error_message,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        jdbcTemplate.update(
            sql,
            metric.requestId,
            metric.serviceName,
            metric.selectedInstance,
            metric.targetUrl,
            metric.statusCode,
            metric.success,
            metric.latencyMs,
            metric.errorMessage,
            Timestamp.from(metric.createdAt)
        )
    }

    fun findRecent(limit: Int = 20): List<Map<String, Any>> {
        val sql = """
            SELECT request_id, service_name, selected_instance, target_url,
                   status_code, success, latency_ms, error_message, created_at
            FROM request_metrics
            ORDER BY created_at DESC
            LIMIT ?
        """.trimIndent()

        return jdbcTemplate.queryForList(sql, limit)
    }
}