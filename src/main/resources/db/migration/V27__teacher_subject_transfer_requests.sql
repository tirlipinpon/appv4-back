-- SCRUM-189 : demande de déplacement / partage à valider par le collègue.
-- Tant que status = pending, les accès matières ne changent pas.

CREATE TABLE back_teacher_subject_transfer_requests (
    id            BIGSERIAL PRIMARY KEY,
    subject_id    BIGINT NOT NULL REFERENCES back_teacher_subjects (id) ON DELETE CASCADE,
    from_user_id  BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    to_user_id    BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    mode          VARCHAR(10) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_transfer_request_mode CHECK (mode IN ('move', 'share')),
    CONSTRAINT ck_transfer_request_status CHECK (status IN ('pending', 'accepted', 'rejected', 'cancelled'))
);

CREATE UNIQUE INDEX uq_transfer_request_pending_target
    ON back_teacher_subject_transfer_requests (subject_id, to_user_id)
    WHERE status = 'pending';

CREATE INDEX idx_transfer_request_to_pending
    ON back_teacher_subject_transfer_requests (to_user_id)
    WHERE status = 'pending';

CREATE INDEX idx_transfer_request_subject
    ON back_teacher_subject_transfer_requests (subject_id);

COMMENT ON TABLE back_teacher_subject_transfer_requests IS
    'Demande de déplacer ou partager une matière. Le collègue doit accepter.';

COMMENT ON COLUMN back_teacher_subject_transfer_requests.mode IS
    'move : le demandeur quitte ; share : les deux gardent l''accès.';

COMMENT ON COLUMN back_teacher_subject_transfer_requests.status IS
    'pending jusqu''à acceptation, refus ou annulation.';
