-- SCRUM-179 : phrase à trous préparée par le professeur.
-- L'énoncé peut rester vide. L'élève ne place pas encore les mots.

CREATE TABLE back_subject_question_gaps (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    sentence     VARCHAR(500) NOT NULL,
    word_label   VARCHAR(200) NOT NULL,
    decoy        BOOLEAN NOT NULL,
    sort_order   INTEGER NOT NULL,
    CONSTRAINT ck_back_subject_question_gaps_sentence CHECK (char_length(btrim(sentence)) > 0),
    CONSTRAINT ck_back_subject_question_gaps_hole CHECK (position('___' in sentence) > 0),
    CONSTRAINT ck_back_subject_question_gaps_word CHECK (char_length(btrim(word_label)) > 0),
    CONSTRAINT ck_back_subject_question_gaps_position CHECK (sort_order >= 0),
    CONSTRAINT uq_back_subject_question_gaps_position UNIQUE (question_id, decoy, sort_order)
);

COMMENT ON TABLE back_subject_question_gaps IS
    'Trou préparé par le professeur. sentence contient ___. word_label est le mot juste si decoy est faux, sinon un leurre. sort_order suit l''ordre saisi dans chaque groupe.';

COMMENT ON COLUMN back_subject_question_gaps.sentence IS
    'Phrase avec au moins un ___, par exemple Le chat mange une ___ dans le jardin.';

COMMENT ON COLUMN back_subject_question_gaps.word_label IS
    'Mot juste du trou, ou mot de la banque qui ne va pas.';

COMMENT ON COLUMN back_subject_question_gaps.decoy IS
    'Faux pour le mot juste, vrai pour un leurre.';
