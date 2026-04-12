-- Schema for activity log used by JdbcLogService
CREATE TABLE IF NOT EXISTS activity_log (
    id BIGSERIAL PRIMARY KEY,
    timestamp TEXT NOT NULL,
    device TEXT NOT NULL,
    room TEXT NOT NULL,
    action TEXT NOT NULL,
    actor TEXT NOT NULL
);
