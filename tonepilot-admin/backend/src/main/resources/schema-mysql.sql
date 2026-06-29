CREATE TABLE IF NOT EXISTS workflow_run_snapshot (
    run_id VARCHAR(64) PRIMARY KEY,
    photo_id BIGINT,
    status VARCHAR(64),
    provider VARCHAR(64),
    target_style VARCHAR(255),
    current_agent VARCHAR(128),
    storage VARCHAR(64),
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS llm_call_log (
    id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64),
    provider VARCHAR(64),
    model_name VARCHAR(128),
    task_type VARCHAR(64),
    success BOOLEAN,
    latency_ms BIGINT,
    prompt_chars INT,
    response_chars INT,
    prompt_preview TEXT,
    response_preview TEXT,
    error_message TEXT,
    started_at TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_event (
    id VARCHAR(64) PRIMARY KEY,
    event_type VARCHAR(128),
    actor VARCHAR(128),
    run_id VARCHAR(64),
    target_type VARCHAR(64),
    target_id VARCHAR(128),
    detail TEXT,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS domain_snapshot (
    domain_type VARCHAR(64) NOT NULL,
    domain_id VARCHAR(128) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (domain_type, domain_id)
);

CREATE INDEX idx_llm_call_log_started_at ON llm_call_log(started_at);
CREATE INDEX idx_audit_event_created_at ON audit_event(created_at);
CREATE INDEX idx_domain_snapshot_type ON domain_snapshot(domain_type);
