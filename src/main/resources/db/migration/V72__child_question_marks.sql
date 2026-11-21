-- Étoile figée sur la tentative. Une relance du même envoi renvoie cette marque, pas une marque plus tardive.
ALTER TABLE back_child_attempts
    ADD COLUMN mark_code VARCHAR(16),
    ADD COLUMN yellow_count INTEGER;

ALTER TABLE back_child_attempts
    ADD CONSTRAINT ck_back_child_attempts_mark CHECK (
        (mark_code IS NULL AND yellow_count IS NULL)
        OR (mark_code IN ('none', 'black', 'yellow') AND yellow_count >= 0)
    );

COMMENT ON COLUMN back_child_attempts.mark_code IS
    'Étoile après cet essai : none, black ou yellow. Null pour une tentative ancienne, relue comme none.';
COMMENT ON COLUMN back_child_attempts.yellow_count IS
    'Nombre d''étoiles jaunes après cet essai. Zéro s''il n''y en a pas. Null pour une tentative ancienne.';

-- Marque courante d'une question pour un enfant. Pas de ligne = pas d'étoile.
CREATE TABLE back_child_question_marks (
    child_id     BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    question_id  BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    mark_code    VARCHAR(16) NOT NULL,
    yellow_count INTEGER NOT NULL,
    PRIMARY KEY (child_id, question_id),
    CONSTRAINT ck_back_child_question_marks_shape CHECK (
        (mark_code = 'black' AND yellow_count = 0)
        OR (mark_code = 'yellow' AND yellow_count >= 1)
    )
);

COMMENT ON TABLE back_child_question_marks IS
    'Étoile d''une question pour un enfant. Noire : une page ratée jusqu''au bout. Jaune : réussie au premier essai, le compte augmente ensuite.';
