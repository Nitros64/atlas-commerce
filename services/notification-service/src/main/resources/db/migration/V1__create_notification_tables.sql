CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    recipient VARCHAR(150) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notification_user_id ON notifications(user_id);
CREATE INDEX idx_notification_status ON notifications(status);