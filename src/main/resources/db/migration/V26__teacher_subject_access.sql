-- SCRUM-172 : plusieurs professeurs sur la même matière (déplacer / partager).
-- Une seule ligne back_teacher_subjects. Les accès sont dans cette table.
-- Les formes de questions et les liens enfants restent sur subject_id.

CREATE TABLE back_teacher_subject_access (
    id          BIGSERIAL PRIMARY KEY,
    subject_id  BIGINT NOT NULL REFERENCES back_teacher_subjects (id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    CONSTRAINT uq_back_teacher_subject_access UNIQUE (subject_id, user_id)
);

CREATE INDEX idx_back_teacher_subject_access_user
    ON back_teacher_subject_access (user_id);

CREATE INDEX idx_back_teacher_subject_access_subject
    ON back_teacher_subject_access (subject_id);

COMMENT ON TABLE back_teacher_subject_access IS
    'Professeurs qui voient et agissent sur une matière. Pas une copie de la matière.';

COMMENT ON COLUMN back_teacher_subject_access.subject_id IS
    'Matière partagée ou déplacée. Même id pour tous les accès.';

COMMENT ON COLUMN back_teacher_subject_access.user_id IS
    'Compte professeur accepté qui a accès à cette matière.';

-- Recopie un accès pour chaque matière encore affichée (user_id actuel).
INSERT INTO back_teacher_subject_access (subject_id, user_id)
SELECT id, user_id
FROM back_teacher_subjects
WHERE hidden = FALSE;
