CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    event_type VARCHAR(50) NOT NULL,
    action VARCHAR(200) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    details VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_user_id
ON audit_events(user_id);

CREATE INDEX idx_audit_event_type
ON audit_events(event_type);