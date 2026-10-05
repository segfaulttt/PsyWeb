CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE slots
ADD CONSTRAINT no_overlapping_active_slots
EXCLUDE USING gist (specialist_id WITH =, tsrange(start_time, end_time, '[)') WITH &&)
WHERE (availability_status IN ('FREE', 'RESERVED', 'BOOKED'));