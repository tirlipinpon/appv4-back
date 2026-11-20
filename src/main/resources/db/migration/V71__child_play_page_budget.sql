-- Budget d'essais d'une page de jeu. Une nouvelle page (nouvel identifiant) repart de zéro.
CREATE TABLE back_child_play_pages (
    play_id        UUID PRIMARY KEY,
    child_id       BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    subject_id     BIGINT NOT NULL REFERENCES back_teacher_subjects (id) ON DELETE CASCADE,
    question_id    BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    attempts_used  INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_back_child_play_pages_used CHECK (attempts_used >= 0)
);

COMMENT ON TABLE back_child_play_pages IS
    'Essais déjà pris pendant qu''une page de jeu est ouverte. Le compteur ne survit pas à une nouvelle page.';

ALTER TABLE back_child_attempts
    ADD COLUMN attempt_rank INTEGER,
    ADD COLUMN attempts_remaining INTEGER,
    ADD COLUMN points_awarded INTEGER;

COMMENT ON COLUMN back_child_attempts.attempt_rank IS
    'Rang de l''essai dans la page, 1 pour le premier. Null pour une tentative ancienne.';
COMMENT ON COLUMN back_child_attempts.attempts_remaining IS
    'Essais encore ouverts après celui-ci. Null pour une tentative ancienne.';
COMMENT ON COLUMN back_child_attempts.points_awarded IS
    'Points du barème de cet essai, 0 si la réponse n''est pas juste. Null pour une tentative ancienne.';
