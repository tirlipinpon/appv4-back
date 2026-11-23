-- SCRUM-161 : désactiver et retirer ne sont pas le même geste, et aucune des deux n'efface la ligne.
-- active = false : le professeur la voit encore, le reste de l'application non.
-- hidden = true : elle disparaît aussi de sa page. La ligne reste.

ALTER TABLE back_teacher_subjects
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE back_teacher_subjects
    DROP CONSTRAINT uq_back_teacher_subjects_owner_class_name;

CREATE UNIQUE INDEX uq_back_teacher_subjects_visible_name
    ON back_teacher_subjects (user_id, school_id, grade_label, name_key)
    WHERE hidden = FALSE;

COMMENT ON COLUMN back_teacher_subjects.active IS
    'Faux : la matière reste affichée chez le professeur, elle n''est plus proposée dans le reste de l''application.';

COMMENT ON COLUMN back_teacher_subjects.hidden IS
    'Vrai : la matière n''est plus affichée. La ligne n''est pas effacée.';
