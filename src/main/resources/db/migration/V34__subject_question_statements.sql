-- SCRUM-177 : phrases vraies ou fausses, préparées par le professeur.
-- L'énoncé peut rester vide. L'élève ne répond pas encore.

CREATE TABLE back_subject_question_statements (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    label        VARCHAR(200) NOT NULL,
    truthful     BOOLEAN NOT NULL,
    sort_order   INTEGER NOT NULL,
    CONSTRAINT ck_back_subject_question_statements_label CHECK (char_length(btrim(label)) > 0),
    CONSTRAINT ck_back_subject_question_statements_position CHECK (sort_order >= 0),
    CONSTRAINT uq_back_subject_question_statements_position UNIQUE (question_id, sort_order)
);

COMMENT ON TABLE back_subject_question_statements IS
    'Phrase d''une question Vrai ou faux. sort_order est l''ordre saisi. truthful dit si la phrase est vraie. L''élève ne répond pas encore.';

COMMENT ON COLUMN back_subject_question_statements.label IS
    'Texte de la phrase, par exemple 2 + 2 = 4.';

COMMENT ON COLUMN back_subject_question_statements.truthful IS
    'Vrai si la phrase est vraie, faux si elle est fausse.';
