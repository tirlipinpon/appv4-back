-- Progression enfant : XP, streak, semaine (BACK-17).
CREATE TABLE back_child_progress (
    child_id        BIGINT PRIMARY KEY REFERENCES back_children (id) ON DELETE CASCADE,
    xp_total        INTEGER NOT NULL DEFAULT 0,
    current_streak  INTEGER NOT NULL DEFAULT 0,
    last_play_date  DATE,
    week_start_date DATE,
    week_days       CHAR(7) NOT NULL DEFAULT '0000000',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_back_child_progress_xp CHECK (xp_total >= 0),
    CONSTRAINT ck_back_child_progress_streak CHECK (current_streak >= 0),
    CONSTRAINT ck_back_child_progress_week_days CHECK (week_days ~ '^[01]{7}$')
);

COMMENT ON TABLE back_child_progress IS
    'XP, streak et jours joués de la semaine ISO pour un enfant (BACK-17).';

COMMENT ON COLUMN back_child_progress.week_days IS
    '7 bits lun→dim (0/1) de la semaine dont le lundi est week_start_date.';
