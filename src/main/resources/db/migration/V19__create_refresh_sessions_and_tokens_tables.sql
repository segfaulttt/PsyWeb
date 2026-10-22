CREATE TABLE refresh_sessions (
	id UUID PRIMARY KEY,
	user_id BIGINT NOT NULL,
	created_at TIMESTAMPTZ NOT NULL,
	expires_at TIMESTAMPTZ NOT NULL,
	revoked_at TIMESTAMPTZ,
	last_used_at TIMESTAMPTZ,
	FOREIGN KEY (user_id) REFERENCES users(id)
);

ALTER TABLE refresh_sessions
ADD CONSTRAINT chk_refresh_sessions_expires_after_created
CHECK (expires_at > created_at),

ADD CONSTRAINT chk_refresh_sessions_revoked_after_created
CHECK (revoked_at >= created_at),

ADD CONSTRAINT chk_refresh_sessions_last_used_after_created
CHECK (last_used_at >= created_at),

ADD CONSTRAINT chk_refresh_sessions_revoked_after_last_used
CHECK (revoked_at >= last_used_at),

ADD CONSTRAINT chk_refresh_sessions_last_used_before_expires
CHECK (last_used_at < expires_at);

CREATE TABLE refresh_tokens (
	id UUID PRIMARY KEY,
	session_id UUID NOT NULL,
	token_hash VARCHAR(64) UNIQUE NOT NULL,
	issued_at TIMESTAMPTZ NOT NULL,
	consumed_at TIMESTAMPTZ,
	FOREIGN KEY (session_id) REFERENCES refresh_sessions(id) ON DELETE CASCADE
);

ALTER TABLE refresh_tokens
ADD CONSTRAINT chk_refresh_tokens_consumed_after_issued
CHECK (consumed_at >= issued_at);

CREATE INDEX idx_refresh_sessions_user_id
ON refresh_sessions(user_id);

CREATE UNIQUE INDEX uq_refresh_tokens_current_per_session
ON refresh_tokens(session_id)
WHERE consumed_at is NULL;
