-- Désactivation par accès professeur + alertes dismissibles pour les co-profs.

ALTER TABLE back_teacher_subject_access
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE back_teacher_subject_access a
SET active = s.active
FROM back_teacher_subjects s
WHERE a.subject_id = s.id;

COMMENT ON COLUMN back_teacher_subject_access.active IS
    'Faux : ce professeur voit encore la matière, elle n''est plus proposée pour lui ailleurs.';

CREATE TABLE back_teacher_subject_deactivate_notices (
    id                     BIGSERIAL PRIMARY KEY,
    subject_id             BIGINT NOT NULL REFERENCES back_teacher_subjects (id) ON DELETE CASCADE,
    from_user_id           BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    to_user_id             BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    deactivated_user_ids   VARCHAR(500) NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    dismissed_at           TIMESTAMPTZ
);

CREATE INDEX idx_deactivate_notice_to_open
    ON back_teacher_subject_deactivate_notices (to_user_id)
    WHERE dismissed_at IS NULL;

CREATE INDEX idx_deactivate_notice_subject
    ON back_teacher_subject_deactivate_notices (subject_id);

COMMENT ON TABLE back_teacher_subject_deactivate_notices IS
    'Alerte : le créateur a désactivé la matière pour lui et éventuellement des co-profs.';

COMMENT ON COLUMN back_teacher_subject_deactivate_notices.deactivated_user_ids IS
    'Identifiants concernés, séparés par des virgules (créateur inclus).';
