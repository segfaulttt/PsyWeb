ALTER TABLE specialists
DROP CONSTRAINT chk_approval_status;

ALTER TABLE specialists
ADD CONSTRAINT chk_approval_status
CHECK (approval_status IN ('PENDING', 'APPROVAL', 'REJECTED', 'SUSPENDED'));