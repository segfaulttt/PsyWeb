
-- Initialize schedule settings for specialists created before V20.
-- Existing schedule settings are preserved.

INSERT INTO specialist_schedule_settings (
    specialist_id,
    mode,
    booking_horizon_days,
    updated_at
)
SELECT
    s.user_id,
    'REGULAR',
    30,
    NOW()
FROM specialists s
WHERE NOT EXISTS (
    SELECT 1
    FROM specialist_schedule_settings settings
    WHERE settings.specialist_id = s.user_id
);

-- Legacy specialists have no stored IANA time zone.
-- UTC is a technical fallback, not a verified local time zone.
-- The specialist must confirm or update it before using
-- the new scheduling functionality.

INSERT INTO specialist_time_zone_guards (
    specialist_id,
    time_zone,
    updated_at
)
SELECT
    s.user_id,
    'UTC',
    NOW()
FROM specialists s
WHERE NOT EXISTS (
    SELECT 1
    FROM specialist_time_zone_guards guard
    WHERE guard.specialist_id = s.user_id
);