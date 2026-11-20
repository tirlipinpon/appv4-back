-- Essais, difficulté et points d'une question. Défaut : 1 essai, Facile, gamification allumée.
ALTER TABLE back_subject_questions
    ADD COLUMN max_attempts INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN difficulty VARCHAR(20) NOT NULL DEFAULT 'facile',
    ADD COLUMN customize_points BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN gamification_disabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE back_subject_questions
    ADD CONSTRAINT ck_back_subject_questions_max_attempts CHECK (max_attempts BETWEEN 1 AND 5),
    ADD CONSTRAINT ck_back_subject_questions_difficulty CHECK (difficulty IN ('facile', 'modere', 'difficile'));

COMMENT ON COLUMN back_subject_questions.max_attempts IS
    'Nombre maximum d''essais. Défaut 1, pour que le professeur n''ait rien à régler.';
COMMENT ON COLUMN back_subject_questions.difficulty IS
    'facile, modere ou difficile. Le barème des points est dans le code, pas saisi ici.';
COMMENT ON COLUMN back_subject_questions.customize_points IS
    'Vrai : les points de chaque essai sont dans back_subject_question_attempt_points.';
COMMENT ON COLUMN back_subject_questions.gamification_disabled IS
    'Vrai : la question reste jouable et dans les statistiques, et elle garde ses points.';

CREATE TABLE back_subject_question_attempt_points (
    question_id  BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    sort_order   INTEGER NOT NULL,
    points       INTEGER NOT NULL,
    PRIMARY KEY (question_id, sort_order),
    CONSTRAINT ck_back_subject_question_attempt_points_points CHECK (points >= 0)
);

COMMENT ON TABLE back_subject_question_attempt_points IS
    'Points saisis par essai, seulement quand customize_points est vrai. L''ordre 0 est le premier essai.';
