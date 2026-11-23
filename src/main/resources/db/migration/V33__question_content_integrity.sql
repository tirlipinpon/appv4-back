-- Les structures existantes restent spécialisées et conservent leurs données.
-- La forme enregistrée sur une question doit exister pour cette matière.
ALTER TABLE back_subject_questions
    ADD CONSTRAINT fk_back_subject_questions_form
    FOREIGN KEY (subject_id, form_code)
    REFERENCES back_subject_question_forms (subject_id, form_code);

CREATE INDEX idx_back_subject_questions_subject_form
    ON back_subject_questions (subject_id, form_code, id);

-- Une seule position par contenu. Les index uniques remplacent les anciens
-- index de lecture, avec le même préfixe (question_id, sort_order).
ALTER TABLE back_subject_question_choices
    ADD CONSTRAINT uq_back_subject_question_choices_position UNIQUE (question_id, sort_order),
    ADD CONSTRAINT ck_back_subject_question_choices_position CHECK (sort_order >= 0);
DROP INDEX idx_back_subject_question_choices_question;

ALTER TABLE back_subject_question_pairs
    ADD CONSTRAINT uq_back_subject_question_pairs_position UNIQUE (question_id, sort_order),
    ADD CONSTRAINT ck_back_subject_question_pairs_position CHECK (sort_order >= 0);
DROP INDEX idx_back_subject_question_pairs_question;

ALTER TABLE back_subject_question_items
    ADD CONSTRAINT uq_back_subject_question_items_position UNIQUE (question_id, sort_order),
    ADD CONSTRAINT ck_back_subject_question_items_position CHECK (sort_order >= 0);
DROP INDEX idx_back_subject_question_items_question;
