ALTER TABLE bookings RENAME COLUMN user_id TO client_id;

ALTER TABLE bookings
ADD COLUMN specialist_id BIGINT NOT NULL;

ALTER TABLE bookings 
ADD COLUMN cancelled_at TIMESTAMP;

ALTER TABLE bookings
ADD CONSTRAINT chk_booking_status 
CHECK (status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'));

ALTER TABLE bookings
ADD CONSTRAINT fk_bookings_specialists
FOREIGN KEY (specialist_id) REFERENCES specialists(user_id);

