-- Mission du jour d'un enfant (BACK-18).
-- Une ligne par enfant et par jour. Les tâches pointent une matière liée et une question jouable.

CREATE TABLE back_child_daily_missions (
    id            BIGSERIAL PRIMARY KEY,
    child_id      BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    mission_date  DATE NOT NULL,
    objective     VARCHAR(120) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_back_child_daily_mission_day UNIQUE (child_id, mission_date),
    CONSTRAINT ck_back_child_daily_mission_objective CHECK (char_length(btrim(objective)) > 0)
);

CREATE TABLE back_child_daily_tasks (
    id              BIGSERIAL PRIMARY KEY,
    mission_id      BIGINT NOT NULL REFERENCES back_child_daily_missions (id) ON DELETE CASCADE,
    position        INTEGER NOT NULL,
    label           VARCHAR(100) NOT NULL,
    target_count    INTEGER NOT NULL,
    progress_count  INTEGER NOT NULL DEFAULT 0,
    reward_xp       INTEGER NOT NULL,
    subject_id      BIGINT NOT NULL REFERENCES back_teacher_subjects (id) ON DELETE CASCADE,
    question_id     BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    CONSTRAINT uq_back_child_daily_task_pos UNIQUE (mission_id, position),
    CONSTRAINT ck_back_child_daily_task_target CHECK (target_count > 0),
    CONSTRAINT ck_back_child_daily_task_progress CHECK (progress_count >= 0 AND progress_count <= target_count),
    CONSTRAINT ck_back_child_daily_task_reward CHECK (reward_xp >= 0),
    CONSTRAINT ck_back_child_daily_task_label CHECK (char_length(btrim(label)) > 0)
);

CREATE INDEX idx_back_child_daily_tasks_mission ON back_child_daily_tasks (mission_id);

COMMENT ON TABLE back_child_daily_missions IS
    'Mission du jour d''un enfant : un objectif stable pour la date mission_date (BACK-18).';

COMMENT ON TABLE back_child_daily_tasks IS
    'Tâches de la mission : libellé, cible, progression, XP et question à jouer.';

COMMENT ON COLUMN back_child_daily_missions.mission_date IS
    'Jour métier Europe/Paris. Deux lectures le même jour retrouvent cette ligne.';
