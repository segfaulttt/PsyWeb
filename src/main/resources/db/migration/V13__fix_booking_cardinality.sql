ALTER TABLE bookings
DROP CONSTRAINT bookings_slot_id_key;

CREATE UNIQUE INDEX unique_confirmed_booking_slot
ON bookings(slot_id)
WHERE status = 'CONFIRMED';

ALTER TABLE bookings
ADD CONSTRAINT uq_bookings_reservation_id
UNIQUE (reservation_id);