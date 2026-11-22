-- Demande de partage (BACK-75). Pas d'identifiant d'enfant : le parent référent choisit.

CREATE TABLE back_child_share_requests (
    id                BIGSERIAL PRIMARY KEY,
    sender_user_id    BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    referent_user_id  BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    status            VARCHAR(16) NOT NULL DEFAULT 'pending',
    CONSTRAINT ck_back_child_share_requests_status
        CHECK (status IN ('pending', 'accepted', 'refused', 'cancelled'))
);

CREATE UNIQUE INDEX uq_back_child_share_requests_pending
    ON back_child_share_requests (sender_user_id, referent_user_id, lower(first_name), lower(last_name))
    WHERE status = 'pending';

COMMENT ON TABLE back_child_share_requests IS
    'Demande de partage : email du parent référent, prénom et nom. Rien n''est partagé avant l''acceptation.';
