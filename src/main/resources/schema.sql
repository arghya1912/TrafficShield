CREATE TABLE IF NOT EXISTS request_metrics (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    selected_instance VARCHAR(100) NOT NULL,
    target_url TEXT NOT NULL,
    status_code INT NOT NULL,
    success BOOLEAN NOT NULL,
    latency_ms BIGINT NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_request_metrics_service_name
ON request_metrics(service_name);

CREATE INDEX IF NOT EXISTS idx_request_metrics_created_at
ON request_metrics(created_at);

CREATE INDEX IF NOT EXISTS idx_request_metrics_success
ON request_metrics(success);

ALTER TABLE request_metrics
ADD COLUMN IF NOT EXISTS outcome_type VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN';

CREATE INDEX IF NOT EXISTS idx_request_metrics_outcome_type
ON request_metrics(outcome_type);