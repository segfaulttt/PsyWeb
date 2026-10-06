ALTER TABLE slots
    ADD COLUMN minimum_booking_notice_minutes INTEGER;

ALTER TABLE slots
    ADD CONSTRAINT chk_slots_minimum_booking_notice_non_negative
        CHECK (
            minimum_booking_notice_minutes IS NULL
            OR minimum_booking_notice_minutes >= 0
        );