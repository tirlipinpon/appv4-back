-- Demande de partage initiée par un professeur qui veut une matière déjà créée.
-- Le créateur accepte ou refuse. Plusieurs collègues peuvent demander la même matière.

ALTER TABLE back_teacher_subject_transfer_requests
    DROP CONSTRAINT ck_transfer_request_mode;

ALTER TABLE back_teacher_subject_transfer_requests
    ADD CONSTRAINT ck_transfer_request_mode CHECK (mode IN ('move', 'share', 'ask'));

DROP INDEX uq_transfer_request_pending_target;

CREATE UNIQUE INDEX uq_transfer_request_pending_pair
    ON back_teacher_subject_transfer_requests (subject_id, from_user_id, to_user_id)
    WHERE status = 'pending';

COMMENT ON COLUMN back_teacher_subject_transfer_requests.mode IS
    'move : le demandeur quitte ; share : les deux gardent l''accès ; ask : un collègue demande au créateur de partager.';
