-- Une école et une année n'ont qu'une matière visible de ce nom, quel que soit le professeur.

DROP INDEX uq_back_teacher_subjects_visible_name;

CREATE UNIQUE INDEX uq_back_teacher_subjects_class_name
    ON back_teacher_subjects (school_id, grade_label, name_key)
    WHERE hidden = FALSE;

COMMENT ON INDEX uq_back_teacher_subjects_class_name IS
    'Nom visible unique pour une école et une année. Un second professeur ne crée pas la même matière. Une matière cachée ne bloque pas le nom.';
