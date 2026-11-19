-- BACK-54 / ADMIN-25 : Simon. Le professeur choisit un genre d'éléments
-- (couleurs, nombres, lettres ou formes), pose au moins deux éléments,
-- puis une suite d'au moins deux pas. L'élève regarde la suite, puis la refait.
-- Un pas désigne un élément par sa position : la clé étrangère composée
-- garantit qu'il pointe un élément existant de la même question.

CREATE TABLE back_subject_question_simon_options (
    question_id   BIGINT PRIMARY KEY REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    element_type  VARCHAR(20) NOT NULL,
    CONSTRAINT ck_back_subject_question_simon_options_element_type
        CHECK (element_type IN ('couleurs', 'nombres', 'lettres', 'formes'))
);

COMMENT ON TABLE back_subject_question_simon_options IS
    'Genre des éléments d''une question Simon : couleurs, nombres, lettres ou formes.';

CREATE TABLE back_subject_question_simon_elements (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    label        VARCHAR(200) NOT NULL,
    color        VARCHAR(7),
    sort_order   INTEGER NOT NULL,
    CONSTRAINT ck_back_subject_question_simon_elements_label CHECK (char_length(btrim(label)) > 0),
    CONSTRAINT ck_back_subject_question_simon_elements_color CHECK (color IS NULL OR color ~ '^#[0-9a-f]{6}$'),
    CONSTRAINT ck_back_subject_question_simon_elements_position CHECK (sort_order >= 0),
    CONSTRAINT uq_back_subject_question_simon_elements_position UNIQUE (question_id, sort_order)
);

COMMENT ON TABLE back_subject_question_simon_elements IS
    'Élément (bouton) d''une question Simon. sort_order est sa position : c''est elle que les pas de la suite désignent.';

COMMENT ON COLUMN back_subject_question_simon_elements.label IS
    'Nom affiché de l''élément, par exemple rouge, 7, B ou cercle.';

COMMENT ON COLUMN back_subject_question_simon_elements.color IS
    'Code couleur #rrggbb en minuscules, seulement pour le genre couleurs. Vide pour les autres genres.';

CREATE TABLE back_subject_question_simon_steps (
    id             BIGSERIAL PRIMARY KEY,
    question_id    BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    element_index  INTEGER NOT NULL,
    sort_order     INTEGER NOT NULL,
    CONSTRAINT ck_back_subject_question_simon_steps_position CHECK (sort_order >= 0),
    CONSTRAINT uq_back_subject_question_simon_steps_position UNIQUE (question_id, sort_order),
    CONSTRAINT fk_back_subject_question_simon_steps_element
        FOREIGN KEY (question_id, element_index)
        REFERENCES back_subject_question_simon_elements (question_id, sort_order) ON DELETE CASCADE
);

COMMENT ON TABLE back_subject_question_simon_steps IS
    'Suite d''une question Simon. sort_order est l''ordre des pas, du premier au dernier.';

COMMENT ON COLUMN back_subject_question_simon_steps.element_index IS
    'Position (sort_order) de l''élément qui s''allume à ce pas. Le même élément peut revenir.';
