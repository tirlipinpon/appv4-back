-- BACK-55 / ADMIN-62 : Simon. Le professeur ne choisit plus la suite : il pose
-- seulement le genre et les éléments. Le serveur tire une suite au hasard chaque
-- fois qu'un enfant ouvre la question, la garde ici, puis note la reprise contre
-- elle. Les suites saisies par le professeur ne servent plus : leur table part.

DROP TABLE back_subject_question_simon_steps;

COMMENT ON TABLE back_subject_question_simon_elements IS
    'Élément (bouton) d''une question Simon. sort_order est sa position : c''est elle que la suite tirée pour l''enfant désigne.';

CREATE TABLE back_child_simon_challenges (
    child_id     BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    question_id  BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    sequence     INTEGER[] NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_back_child_simon_challenges PRIMARY KEY (child_id, question_id),
    CONSTRAINT ck_back_child_simon_challenges_sequence CHECK (
        cardinality(sequence) > 0
        AND array_position(sequence, NULL) IS NULL
        AND 0 <= ALL (sequence))
);

COMMENT ON TABLE back_child_simon_challenges IS
    'Dernière suite Simon montrée à un enfant pour une question. Remplacée à chaque ouverture, retirée après la note.';

COMMENT ON COLUMN back_child_simon_challenges.sequence IS
    'Positions (sort_order) des éléments qui s''allument, du premier pas au dernier. Un élément peut revenir.';
