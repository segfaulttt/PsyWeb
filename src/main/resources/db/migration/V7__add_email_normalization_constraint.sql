ALTER TABLE users
ADD CONSTRAINT chk_users_email_lowercase
CHECK (email = lower(email));