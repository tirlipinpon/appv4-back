-- SCRUM-178 : mot de gauche déjà relié à sa réponse de droite.
-- L'énoncé peut rester vide. L'élève ne trace pas encore le lien.

CREATE TABLE back_subject_question_links (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    left_label   VARCHAR(200) NOT NULL,
    right_label  VARCHAR(200) NOT NULL,
    sort_order   INTEGER NOT NULL,
    CONSTRAINT ck_back_subject_question_links_left CHECK (char_length(btrim(left_label)) > 0),
    CONSTRAINT ck_back_subject_question_links_right CHECK (char_length(btrim(right_label)) > 0),
    CONSTRAINT ck_back_subject_question_links_position CHECK (sort_order >= 0),
    CONSTRAINT uq_back_subject_question_links_position UNIQUE (question_id, sort_order)
);

COMMENT ON TABLE back_subject_question_links IS
    'Lien préparé par le professeur. left_label est le mot, right_label sa réponse. sort_order est l''ordre saisi. L''élève ne trace pas encore le trait.';

COMMENT ON COLUMN back_subject_question_links.left_label IS
    'Mot de gauche, par exemple Pomme.';

COMMENT ON COLUMN back_subject_question_links.right_label IS
    'Réponse de droite, par exemple Fruit.';
