-- Schema for in-app notifications used by JdbcNotificationService (FR-12).
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    timestamp VARCHAR(32) NOT NULL,
    message VARCHAR(1024) NOT NULL,
    notification_type VARCHAR(32) NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notifications_type_chk CHECK (notification_type IN ('INFO', 'SUCCESS', 'FAILURE', 'WARNING'))
);

CREATE INDEX IF NOT EXISTS idx_notifications_read_flag ON notifications (read_flag);
