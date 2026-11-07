CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,

    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT uq_password_reset_tokens_hash
        UNIQUE (token_hash),

    CONSTRAINT chk_password_reset_tokens_lifetime
        CHECK (expires_at > created_at),

    CONSTRAINT chk_password_reset_tokens_consumed
        CHECK (consumed_at IS NULL OR (
            consumed_at >= created_at
            AND consumed_at < expires_at
        )),

    CONSTRAINT chk_password_reset_tokens_revoked
        CHECK (revoked_at IS NULL OR revoked_at >= created_at),

    CONSTRAINT chk_password_reset_tokens_terminal_state
        CHECK (consumed_at IS NULL OR revoked_at IS NULL)
);

CREATE INDEX idx_password_reset_tokens_user_id
    ON password_reset_tokens(user_id);

CREATE INDEX idx_password_reset_tokens_expires_at
    ON password_reset_tokens(expires_at);