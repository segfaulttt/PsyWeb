CREATE EXTENSION IF NOT EXISTS btree_gist;


CREATE TABLE specialist_schedule_settings (
    specialist_id BIGINT PRIMARY KEY,

    mode VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    booking_horizon_days INTEGER NOT NULL DEFAULT 30,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_schedule_settings_specialist
        FOREIGN KEY (specialist_id)
        REFERENCES specialists(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_schedule_mode
        CHECK (mode IN ('REGULAR', 'FLOATING')),

    CONSTRAINT chk_booking_horizon
        CHECK (booking_horizon_days IN (14, 30, 60, 90))
);


CREATE TABLE specialist_time_zone_guards (
    specialist_id BIGINT PRIMARY KEY,

    time_zone VARCHAR(100) NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_time_zone_guard_specialist
        FOREIGN KEY (specialist_id)
        REFERENCES specialists(user_id)
        ON DELETE RESTRICT
);


CREATE TABLE weekly_schedule_versions (
    id UUID PRIMARY KEY,

    specialist_id BIGINT NOT NULL,

    effective_from DATE NOT NULL,
    effective_until DATE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_weekly_version_specialist
        FOREIGN KEY (specialist_id)
        REFERENCES specialists(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_weekly_version_period
        CHECK (
            effective_until IS NULL
            OR effective_until >= effective_from
        ),

    CONSTRAINT ex_weekly_schedule_versions
        EXCLUDE USING gist (
            specialist_id WITH =,
            daterange(
                effective_from,
                effective_until,
                '[]'
            ) WITH &&
        )
        DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_weekly_versions_specialist
    ON weekly_schedule_versions(specialist_id);


CREATE TABLE weekly_working_intervals (
    id UUID PRIMARY KEY,

    schedule_version_id UUID NOT NULL,

    day_of_week SMALLINT NOT NULL,

    start_time TIME(0) NOT NULL,
    end_time TIME(0) NOT NULL,

    start_second INTEGER GENERATED ALWAYS AS (
        EXTRACT(EPOCH FROM start_time)::INTEGER
    ) STORED,

    end_second INTEGER GENERATED ALWAYS AS (
        EXTRACT(EPOCH FROM end_time)::INTEGER
    ) STORED,

    CONSTRAINT fk_weekly_interval_version
        FOREIGN KEY (schedule_version_id)
        REFERENCES weekly_schedule_versions(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_weekly_day
        CHECK (day_of_week BETWEEN 1 AND 7),

    CONSTRAINT chk_weekly_time
        CHECK (start_time < end_time),

    CONSTRAINT ex_weekly_working_intervals
        EXCLUDE USING gist (
            schedule_version_id WITH =,
            day_of_week WITH =,
            int4range(
                start_second,
                end_second,
                '[)'
            ) WITH &&
        )
        DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_weekly_intervals_version
    ON weekly_working_intervals(schedule_version_id);


CREATE TABLE date_schedules (
    id UUID PRIMARY KEY,

    specialist_id BIGINT NOT NULL,

    schedule_date DATE NOT NULL,
    purpose VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_date_schedule_specialist
        FOREIGN KEY (specialist_id)
        REFERENCES specialists(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_date_schedule_purpose
        CHECK (purpose IN ('MANUAL', 'OVERRIDE')),

    CONSTRAINT uq_date_schedule
        UNIQUE (
            specialist_id,
            schedule_date,
            purpose
        )
);

CREATE INDEX idx_date_schedules_specialist_date
    ON date_schedules(specialist_id, schedule_date);


CREATE TABLE date_schedule_intervals (
    id UUID PRIMARY KEY,

    date_schedule_id UUID NOT NULL,

    start_time TIME(0) NOT NULL,
    end_time TIME(0) NOT NULL,

    start_second INTEGER GENERATED ALWAYS AS (
        EXTRACT(EPOCH FROM start_time)::INTEGER
    ) STORED,

    end_second INTEGER GENERATED ALWAYS AS (
        EXTRACT(EPOCH FROM end_time)::INTEGER
    ) STORED,

    CONSTRAINT fk_date_interval_schedule
        FOREIGN KEY (date_schedule_id)
        REFERENCES date_schedules(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_date_interval_time
        CHECK (start_time < end_time),

    CONSTRAINT ex_date_schedule_intervals
        EXCLUDE USING gist (
            date_schedule_id WITH =,
            int4range(
                start_second,
                end_second,
                '[)'
            ) WITH &&
        )
        DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_date_intervals_schedule
    ON date_schedule_intervals(date_schedule_id);