-- SCRUM-108 : jetons reset mot de passe (forgot-password)

CREATE TABLE back_password_reset_tokens (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    token_hash   VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    used_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_back_password_reset_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_back_password_reset_tokens_user_id ON back_password_reset_tokens (user_id);
CREATE INDEX idx_back_password_reset_tokens_expires_at ON back_password_reset_tokens (expires_at);
