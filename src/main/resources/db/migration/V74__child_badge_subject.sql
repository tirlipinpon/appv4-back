-- Matière qui a donné le badge Matière au jaune (BACK-57).
ALTER TABLE back_child_badges
    ADD COLUMN subject_id BIGINT REFERENCES back_teacher_subjects (id) ON DELETE SET NULL;

COMMENT ON COLUMN back_child_badges.subject_id IS
    'Matière du badge Matière au jaune. Null pour les autres badges, ou si la matière a été supprimée.';
