ALTER TABLE slots
DROP CONSTRAINT IF EXISTS chk_availability_status;


UPDATE slots
SET availability_status = 'RESERVED'
where availability_status = 'BLOCKED';

ALTER TABLE slots
ADD CONSTRAINT chk_availability_status
CHECK (availability_status IN ('FREE', 'RESERVED', 'CANCELLED', 'BOOKED'));