-- SCRUM-182 : saisie lettre par lettre préparée par le professeur.
-- Une case par lettre. La comparaison se fait au moment de Valider, pas dans cette table.

CREATE TABLE back_subject_question_letters (
    question_id      BIGINT PRIMARY KEY REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    expected_answer  VARCHAR(500) NOT NULL,
    CONSTRAINT ck_back_subject_question_letters_answer CHECK (char_length(btrim(expected_answer)) > 0)
);

COMMENT ON TABLE back_subject_question_letters IS
    'Réponse attendue d''une question de forme reponse_libre. Une ligne par question. L''élève a une case par lettre.';

COMMENT ON COLUMN back_subject_question_letters.expected_answer IS
    'Mot juste saisi par le professeur, espaces au début et à la fin déjà retirés. Les majuscules et les accents restent.';
