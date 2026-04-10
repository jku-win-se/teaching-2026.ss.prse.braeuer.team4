CREATE TABLE IF NOT EXISTS scheduled_actions (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    device_name VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    time_pattern VARCHAR(255) NOT NULL,
    recurrence VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_triggered_at TIMESTAMPTZ,
    CONSTRAINT scheduled_actions_recurrence_chk CHECK (recurrence IN ('Daily', 'Weekdays', 'Weekends', 'Weekly'))
);

CREATE INDEX IF NOT EXISTS idx_scheduled_actions_active ON scheduled_actions (active);
CREATE INDEX IF NOT EXISTS idx_scheduled_actions_device_id ON scheduled_actions (device_id);