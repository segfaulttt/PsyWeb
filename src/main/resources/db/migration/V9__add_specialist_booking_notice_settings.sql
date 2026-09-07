ALTER TABLE specialists
	ADD COLUMN minimum_booking_notice_minutes INTEGER NOT NULL DEFAULT 0,
	ADD COLUMN client_cancellation_notice_minutes INTEGER NOT NULL DEFAULT 0;

ALTER TABLE specialists
	ADD CONSTRAINT chk_specialists_minimum_booking_notice_non_negative
		CHECK (minimum_booking_notice_minutes >= 0),
	ADD CONSTRAINT chk_specialists_client_cancellation_notice_non_negative
		CHECK (client_cancellation_notice_minutes >= 0);

ALTER TABLE specialists
	ALTER COLUMN minimum_booking_notice_minutes DROP DEFAULT,
	ALTER COLUMN client_cancellation_notice_minutes DROP DEFAULT;