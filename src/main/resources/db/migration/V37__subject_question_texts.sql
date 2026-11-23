-- SCRUM-198 : réponse libre préparée par le professeur.
-- Un seul champ pour l'élève. La comparaison se fait plus tard, pas dans cette table.

CREATE TABLE back_subject_question_texts (
    question_id      BIGINT PRIMARY KEY REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    expected_answer  VARCHAR(500) NOT NULL,
    CONSTRAINT ck_back_subject_question_texts_answer CHECK (char_length(btrim(expected_answer)) > 0)
);

COMMENT ON TABLE back_subject_question_texts IS
    'Réponse attendue d''une question de forme texte. Une ligne par question. L''élève écrira dans un seul champ.';

COMMENT ON COLUMN back_subject_question_texts.expected_answer IS
    'Texte juste saisi par le professeur, espaces au début et à la fin déjà retirés. Les majuscules et les accents restent.';
