-- SCRUM-176 : liste déjà dans le bon ordre, préparée par le professeur.
-- L'énoncé peut rester vide. L'élève ne remet pas encore les éléments en place.

CREATE TABLE back_subject_question_items (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    label        VARCHAR(200) NOT NULL,
    sort_order   INTEGER NOT NULL,
    CONSTRAINT ck_back_subject_question_items_label CHECK (char_length(btrim(label)) > 0)
);

CREATE INDEX idx_back_subject_question_items_question
    ON back_subject_question_items (question_id, sort_order);

COMMENT ON TABLE back_subject_question_items IS
    'Élément d''une question Ordre. sort_order est l''ordre saisi par le professeur, du premier au dernier. L''élève ne joue pas encore.';

COMMENT ON COLUMN back_subject_question_items.label IS
    'Texte de l''élément, par exemple Lundi.';
