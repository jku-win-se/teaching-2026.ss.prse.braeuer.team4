-- Schema for energy consumption calculations used by JdbcEnergyService
-- Note: This schema is optional. Energy calculations are derived from activity_log entries.
-- This file can be used for future performance optimizations with pre-calculated energy data.

-- Optional table for storing pre-calculated daily energy consumption
CREATE TABLE IF NOT EXISTS energy_daily (
    date DATE NOT NULL,
    device_id TEXT NOT NULL,
    device_name TEXT NOT NULL,
    on_time_hours DECIMAL(10, 2) NOT NULL,
    consumption_wh DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (date, device_id)
);

-- Optional table for storing pre-calculated weekly energy consumption
CREATE TABLE IF NOT EXISTS energy_weekly (
    "year" INTEGER NOT NULL,
    iso_week INTEGER NOT NULL,
    device_id TEXT NOT NULL,
    device_name TEXT NOT NULL,
    on_time_hours DECIMAL(10, 2) NOT NULL,
    consumption_wh DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("year", iso_week, device_id)
);

-- Optional table for device type to nominal power mapping (cache)
CREATE TABLE IF NOT EXISTS device_power_config (
    device_type TEXT PRIMARY KEY,
    nominal_power_w INTEGER NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for efficient queries by device and date
CREATE INDEX IF NOT EXISTS idx_energy_daily_device ON energy_daily(device_name, date);
CREATE INDEX IF NOT EXISTS idx_energy_weekly_device ON energy_weekly(device_name, "year", iso_week);
