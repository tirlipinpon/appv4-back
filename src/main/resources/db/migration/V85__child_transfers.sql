-- Transfert de fiche (BACK-76). Un seul en attente par enfant.

CREATE TABLE back_child_transfers (
    id            BIGSERIAL PRIMARY KEY,
    child_id      BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    from_user_id  BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    to_user_id    BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    message       VARCHAR(200) NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'pending',
    CONSTRAINT ck_back_child_transfers_status
        CHECK (status IN ('pending', 'accepted', 'refused', 'cancelled'))
);

CREATE UNIQUE INDEX uq_back_child_transfers_pending
    ON back_child_transfers (child_id)
    WHERE status = 'pending';

COMMENT ON TABLE back_child_transfers IS
    'Le parent référent propose à un parent déjà sur la fiche d''en devenir le parent référent.';
