-- BACK-46 : ligne du temps optionnelle sur une question Ordre.
-- Une option 1:1 dit si la frise est active. Chaque item peut porter un libellé d'axe (jalon).

CREATE TABLE back_subject_question_order_options (
    question_id       BIGINT PRIMARY KEY REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    timeline_enabled  BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE back_subject_question_order_options IS
    'Options d''une question Ordre : frise (ligne du temps) affichée ou non.';

COMMENT ON COLUMN back_subject_question_order_options.timeline_enabled IS
    'Vrai : l''élève joue sur une frise ; chaque item a un libellé d''axe.';

ALTER TABLE back_subject_question_items
    ADD COLUMN axis_label VARCHAR(80);

COMMENT ON COLUMN back_subject_question_items.axis_label IS
    'Libellé d''axe du jalon (ex. « 20 minutes »). Null si la frise est désactivée.';
