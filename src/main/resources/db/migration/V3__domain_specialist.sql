ALTER TABLE specialists RENAME full_name TO first_name;

ALTER TABLE specialists ADD COLUMN last_name VARCHAR(255);

ALTER TABLE specialists ADD COLUMN approval_status VARCHAR(50) DEFAULT 'PENDING' NOT NULL;

ALTER TABLE specialists 
ADD CONSTRAINT chk_approval_status 
CHECK(approval_status IN ('PENDING', 'APPROVED', 'REJECTED'));

ALTER TABLE slots DROP CONSTRAINT slots_specialist_id_fkey;

ALTER TABLE specialists DROP COLUMN IF EXISTS id;

ALTER TABLE specialists ADD PRIMARY KEY (user_id);

ALTER TABLE specialists 
ADD CONSTRAINT fk_specialists_users
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE specialists ALTER COLUMN last_name SET NOT NULL;