CREATE TABLE security_rate_limits (
    bucket_key VARCHAR(64) PRIMARY KEY,
    attempts INTEGER NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_security_rate_limits_expiry ON security_rate_limits(expires_at);

CREATE TABLE revoked_admin_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_revoked_admin_tokens_expiry ON revoked_admin_tokens(expires_at);
