ALTER TABLE slots
ADD CONSTRAINT fk_slots_specialist
FOREIGN KEY (specialist_id) REFERENCES specialists(user_id);

ALTER TABLE slots RENAME status TO availability_status;

ALTER TABLE slots
ADD CONSTRAINT chk_availability_status 
CHECK(availability_status IN ('FREE', 'BOOKED', 'BLOCKED'));

CREATE INDEX idx_slots_specialist_time
ON slots(specialist_id, start_time, end_time);