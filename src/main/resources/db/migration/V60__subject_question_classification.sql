-- Classement : N colonnes nommées, cartes à ranger chacune dans une colonne juste.

CREATE TABLE back_subject_question_columns (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    label        VARCHAR(200) NOT NULL,
    sort_order   INTEGER NOT NULL,
    CONSTRAINT ck_back_subject_question_columns_label CHECK (char_length(btrim(label)) > 0),
    CONSTRAINT ck_back_subject_question_columns_position CHECK (sort_order >= 0),
    CONSTRAINT uq_back_subject_question_columns_position UNIQUE (question_id, sort_order)
);

COMMENT ON TABLE back_subject_question_columns IS
    'Colonne d''un classement préparée par le professeur. sort_order est l''ordre saisi.';

COMMENT ON COLUMN back_subject_question_columns.label IS
    'Libellé de la colonne, par exemple L''aide peut le financer.';

CREATE TABLE back_subject_question_cards (
    id                  BIGSERIAL PRIMARY KEY,
    question_id         BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    label               VARCHAR(200) NOT NULL,
    correct_column_id   BIGINT NOT NULL REFERENCES back_subject_question_columns (id) ON DELETE CASCADE,
    sort_order          INTEGER NOT NULL,
    CONSTRAINT ck_back_subject_question_cards_label CHECK (char_length(btrim(label)) > 0),
    CONSTRAINT ck_back_subject_question_cards_position CHECK (sort_order >= 0),
    CONSTRAINT uq_back_subject_question_cards_position UNIQUE (question_id, sort_order)
);

COMMENT ON TABLE back_subject_question_cards IS
    'Carte d''un classement. correct_column_id est la colonne juste ; l''élève ne la reçoit pas en jeu.';

COMMENT ON COLUMN back_subject_question_cards.label IS
    'Texte de la carte à classer.';

COMMENT ON COLUMN back_subject_question_cards.correct_column_id IS
    'Colonne correcte pour cette carte (même question).';

CREATE INDEX idx_back_subject_question_cards_column
    ON back_subject_question_cards (correct_column_id);
