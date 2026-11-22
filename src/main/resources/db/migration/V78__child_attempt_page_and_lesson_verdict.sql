ALTER TABLE back_child_attempts
    ADD COLUMN play_id UUID,
    ADD COLUMN lesson_finished BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT ck_back_child_attempts_lesson_finished
        CHECK (NOT lesson_finished OR (passed AND play_id IS NOT NULL));

COMMENT ON COLUMN back_child_attempts.play_id IS
    'Identifiant de la page au moment de l''essai. Absent des essais historiques.';
COMMENT ON COLUMN back_child_attempts.lesson_finished IS
    'Cet essai a vidé la leçon. Conservé pour rendre le même verdict lors d''un renvoi.';
