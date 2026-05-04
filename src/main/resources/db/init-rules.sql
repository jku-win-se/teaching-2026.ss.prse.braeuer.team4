-- Schema for automation rules used by JdbcRuleService (FR-10/FR-11).
CREATE TABLE IF NOT EXISTS rules (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    trigger_type VARCHAR(64) NOT NULL,
    source_device VARCHAR(255) NOT NULL,
    condition_expr VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    target_device VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'Active',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rules_enabled ON rules (enabled);
