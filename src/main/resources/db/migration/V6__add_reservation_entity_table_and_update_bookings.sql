CREATE TABLE reservations (
	id BIGSERIAL PRIMARY KEY,
	client_id BIGINT NOT NULL,
	slot_id BIGINT NOT NULL,
	status VARCHAR(20) NOT NULL,
	created_at TIMESTAMP NOT NULL,
	expires_at TIMESTAMP NOT NULL,
	
	CONSTRAINT fk_reservation_users
	FOREIGN KEY (client_id) REFERENCES users(id),
	
	CONSTRAINT fk_reservation_slots
	FOREIGN KEY(slot_id) REFERENCES slots(id),
	
	CONSTRAINT chk_reservation_status CHECK(status IN ('ACTIVE', 'CONFIRMED', 'EXPIRED', 'CANCELLED'))
)

CREATE UNIQUE INDEX unique_active_reservation_slot
ON reservations(slot_id)
WHERE status = 'ACTIVE';

CREATE INDEX idx_reservation_client
ON reservations(client_id);

ALTER TABLE bookings
ADD COLUMN reservation_id BIGINT NOT NULL;

ALTER TABLE bookings
ADD CONSTRAINT fk_bookings_reservations
FOREIGN KEY (reservation_id) REFERENCES reservations(id);