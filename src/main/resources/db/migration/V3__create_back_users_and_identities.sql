-- SCRUM-93 : socle auth admin (users + identities sociales)

CREATE TABLE back_users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(320) NOT NULL,
    password_hash   VARCHAR(255),
    display_name    VARCHAR(255) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_back_users_email UNIQUE (email)
);

CREATE INDEX idx_back_users_email ON back_users (email);

CREATE TABLE back_user_identities (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    provider            VARCHAR(32) NOT NULL,
    provider_subject    VARCHAR(255) NOT NULL,
    email_at_provider   VARCHAR(320),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_back_user_identities_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT ck_back_user_identities_provider CHECK (provider IN ('google', 'facebook', 'apple'))
);

CREATE INDEX idx_back_user_identities_user_id ON back_user_identities (user_id);
