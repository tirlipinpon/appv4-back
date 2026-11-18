-- Board matière enfant : métadonnées, thèmes et leçons (BACK-33).
-- La progression se calcule à partir des tentatives sur les questions liées.

CREATE TABLE back_subject_board (
    subject_id      BIGINT PRIMARY KEY REFERENCES back_teacher_subjects (id) ON DELETE CASCADE,
    description     VARCHAR(500),
    tagline         VARCHAR(160),
    world_label     VARCHAR(80),
    age_label       VARCHAR(40),
    level_label     VARCHAR(40),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE back_subject_topics (
    id              BIGSERIAL PRIMARY KEY,
    subject_id      BIGINT NOT NULL REFERENCES back_teacher_subjects (id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    icon_code       VARCHAR(128),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_back_subject_topics_name CHECK (char_length(btrim(name)) > 0),
    CONSTRAINT uq_back_subject_topics_pos UNIQUE (subject_id, sort_order)
);

CREATE TABLE back_subject_lessons (
    id                  BIGSERIAL PRIMARY KEY,
    topic_id            BIGINT NOT NULL REFERENCES back_subject_topics (id) ON DELETE CASCADE,
    title               VARCHAR(120) NOT NULL,
    sort_order          INTEGER NOT NULL DEFAULT 0,
    estimated_minutes   INTEGER NOT NULL DEFAULT 5,
    reward_xp           INTEGER NOT NULL DEFAULT 10,
    CONSTRAINT ck_back_subject_lessons_title CHECK (char_length(btrim(title)) > 0),
    CONSTRAINT ck_back_subject_lessons_minutes CHECK (estimated_minutes > 0),
    CONSTRAINT ck_back_subject_lessons_xp CHECK (reward_xp >= 0),
    CONSTRAINT uq_back_subject_lessons_pos UNIQUE (topic_id, sort_order)
);

CREATE TABLE back_subject_lesson_questions (
    lesson_id       BIGINT NOT NULL REFERENCES back_subject_lessons (id) ON DELETE CASCADE,
    question_id     BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (lesson_id, question_id),
    CONSTRAINT uq_back_subject_lesson_questions_pos UNIQUE (lesson_id, sort_order)
);

CREATE INDEX idx_back_subject_topics_subject ON back_subject_topics (subject_id, sort_order);
CREATE INDEX idx_back_subject_lessons_topic ON back_subject_lessons (topic_id, sort_order);
CREATE INDEX idx_back_subject_lesson_questions_question ON back_subject_lesson_questions (question_id);

COMMENT ON TABLE back_subject_board IS
    'Textes du board matière pour Learn (BACK-33). Absent = board minimal.';

COMMENT ON TABLE back_subject_topics IS
    'Thèmes d''une matière affichés sur le board enfant.';

COMMENT ON TABLE back_subject_lessons IS
    'Leçons d''un thème ; le statut enfant se déduit des tentatives.';

COMMENT ON TABLE back_subject_lesson_questions IS
    'Questions d''une leçon, dans l''ordre de jeu.';
