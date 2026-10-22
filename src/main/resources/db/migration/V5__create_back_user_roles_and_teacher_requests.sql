-- SCRUM-121 : rôle parent immédiat, demande professeur en attente

CREATE TABLE back_user_roles (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    role        VARCHAR(32) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_back_user_roles_user_role UNIQUE (user_id, role),
    CONSTRAINT ck_back_user_roles_role CHECK (role IN ('parent', 'teacher', 'admin'))
);

CREATE INDEX idx_back_user_roles_user_id ON back_user_roles (user_id);

CREATE TABLE back_teacher_role_requests (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    status       VARCHAR(32) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at  TIMESTAMPTZ,
    CONSTRAINT ck_back_teacher_role_requests_status CHECK (status IN ('pending', 'approved', 'rejected'))
);

CREATE INDEX idx_back_teacher_role_requests_user_id ON back_teacher_role_requests (user_id);

-- Une seule demande ouverte par compte. L'approbation viendra plus tard.
CREATE UNIQUE INDEX uq_back_teacher_role_requests_one_pending
    ON back_teacher_role_requests (user_id)
    WHERE status = 'pending';
