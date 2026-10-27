-- SCRUM-175 : paires Memory préparées par le professeur.
-- L'énoncé d'un Memory peut rester vide. Le choix multiple continue de l'exiger
-- dans le service. Les paires ne sont pas des propositions à cocher.

ALTER TABLE back_subject_questions DROP CONSTRAINT ck_back_subject_questions_prompt;

COMMENT ON COLUMN back_subject_questions.prompt IS
    'Énoncé. Obligatoire pour un choix multiple. Facultatif pour un Memory : chaîne vide si le professeur n''en saisit pas.';

CREATE TABLE back_subject_question_pairs (
    id             BIGSERIAL PRIMARY KEY,
    question_id    BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    question_side  VARCHAR(200) NOT NULL,
    answer_side    VARCHAR(200) NOT NULL,
    sort_order     INTEGER NOT NULL,
    CONSTRAINT ck_back_subject_question_pairs_question CHECK (char_length(btrim(question_side)) > 0),
    CONSTRAINT ck_back_subject_question_pairs_answer CHECK (char_length(btrim(answer_side)) > 0)
);

CREATE INDEX idx_back_subject_question_pairs_question
    ON back_subject_question_pairs (question_id, sort_order);

COMMENT ON TABLE back_subject_question_pairs IS
    'Paire d''un Memory. question_side et answer_side sont les deux faces de la même carte. L''élève ne joue pas encore.';

COMMENT ON COLUMN back_subject_question_pairs.question_side IS
    'Face question de la paire, par exemple 2 + 2.';

COMMENT ON COLUMN back_subject_question_pairs.answer_side IS
    'Face réponse de la paire, par exemple 4.';
