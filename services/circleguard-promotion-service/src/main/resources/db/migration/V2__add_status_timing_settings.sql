CREATE TABLE IF NOT EXISTS system_settings (
    id BIGSERIAL PRIMARY KEY,
    unconfirmed_fencing_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    auto_threshold_seconds BIGINT NOT NULL DEFAULT 3600,
    mandatory_fence_days INTEGER NOT NULL DEFAULT 14,
    encounter_window_days INTEGER NOT NULL DEFAULT 14
);

ALTER TABLE system_settings
    ADD COLUMN IF NOT EXISTS mandatory_fence_days INTEGER NOT NULL DEFAULT 14,
    ADD COLUMN IF NOT EXISTS encounter_window_days INTEGER NOT NULL DEFAULT 14;

UPDATE system_settings
SET mandatory_fence_days = 14
WHERE mandatory_fence_days IS NULL;

UPDATE system_settings
SET encounter_window_days = 14
WHERE encounter_window_days IS NULL;

INSERT INTO system_settings (
    unconfirmed_fencing_enabled,
    auto_threshold_seconds,
    mandatory_fence_days,
    encounter_window_days
)
SELECT TRUE, 3600, 14, 14
WHERE NOT EXISTS (SELECT 1 FROM system_settings);
