-- Tentatives de jeu enfant (BACK-16). Ticket : back_child_attempts + answers.
CREATE TABLE back_child_attempts (
    id              BIGSERIAL PRIMARY KEY,
    child_id        BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    subject_id      BIGINT NOT NULL REFERENCES back_teacher_subjects (id) ON DELETE CASCADE,
    question_id     BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    form_code       VARCHAR(40) NOT NULL,
    score_percent   SMALLINT NOT NULL,
    passed          BOOLEAN NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_back_child_attempts_score CHECK (score_percent BETWEEN 0 AND 100)
);

CREATE INDEX idx_back_child_attempts_child_created
    ON back_child_attempts (child_id, created_at DESC);

CREATE INDEX idx_back_child_attempts_question
    ON back_child_attempts (question_id);

COMMENT ON TABLE back_child_attempts IS
    'Une partie enfant sur une question : score et verdict (BACK-16).';

CREATE TABLE back_child_attempt_answers (
    id               BIGSERIAL PRIMARY KEY,
    attempt_id       BIGINT NOT NULL REFERENCES back_child_attempts (id) ON DELETE CASCADE,
    target_kind      VARCHAR(32) NOT NULL,
    target_id        BIGINT NOT NULL,
    answer_bool      BOOLEAN,
    answer_choice_id BIGINT,
    correct          BOOLEAN NOT NULL,
    CONSTRAINT ck_back_child_attempt_answers_shape CHECK (
        answer_bool IS NOT NULL OR answer_choice_id IS NOT NULL
    )
);

CREATE INDEX idx_back_child_attempt_answers_attempt
    ON back_child_attempt_answers (attempt_id);

COMMENT ON TABLE back_child_attempt_answers IS
    'Détail des réponses d''une tentative (statement/choice).';
