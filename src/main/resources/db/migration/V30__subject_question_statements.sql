-- SCRUM-174 : énoncé d'un choix multiple, rattaché à une matière.
-- La forme (code qcm) est notée dans back_subject_question_forms, table déjà créée
-- vide en SCRUM-161. Cette migration n'y met aucune ligne : la première question
-- de la matière écrit la forme.

CREATE TABLE back_subject_questions (
    id          BIGSERIAL PRIMARY KEY,
    subject_id  BIGINT NOT NULL REFERENCES back_teacher_subjects (id) ON DELETE CASCADE,
    form_code   VARCHAR(40) NOT NULL,
    prompt      VARCHAR(500) NOT NULL,
    CONSTRAINT ck_back_subject_questions_prompt CHECK (char_length(btrim(prompt)) > 0)
);

CREATE INDEX idx_back_subject_questions_subject ON back_subject_questions (subject_id, id);

COMMENT ON TABLE back_subject_questions IS
    'Énoncé préparé par un professeur dans une matière. La forme est form_code (qcm pour le choix multiple).';

COMMENT ON COLUMN back_subject_questions.form_code IS
    'Code de la forme, le même que back_subject_question_forms.form_code pour cette matière.';

CREATE TABLE back_subject_question_choices (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    label        VARCHAR(200) NOT NULL,
    correct      BOOLEAN NOT NULL,
    sort_order   INTEGER NOT NULL,
    CONSTRAINT ck_back_subject_question_choices_label CHECK (char_length(btrim(label)) > 0)
);

CREATE INDEX idx_back_subject_question_choices_question
    ON back_subject_question_choices (question_id, sort_order);

COMMENT ON TABLE back_subject_question_choices IS
    'Proposition d''un choix multiple. correct vrai = bonne réponse. Une question en a plusieurs.';
