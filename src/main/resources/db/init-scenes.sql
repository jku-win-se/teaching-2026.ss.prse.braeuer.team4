CREATE TABLE IF NOT EXISTS scenes (
    id          VARCHAR(64)  PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scene_device_states (
    id          SERIAL       PRIMARY KEY,
    scene_id    VARCHAR(64)  NOT NULL REFERENCES scenes(id) ON DELETE CASCADE,
    device_state VARCHAR(512) NOT NULL,
    sort_order  INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_scene_device_states_scene_id
    ON scene_device_states (scene_id);
