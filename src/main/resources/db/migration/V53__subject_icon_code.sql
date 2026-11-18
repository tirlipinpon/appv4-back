-- Icône de matière (LEARN-29 / BACK-15) : code de la librairie, choisi par le professeur.
ALTER TABLE back_teacher_subjects
    ADD COLUMN icon_code VARCHAR(32);

COMMENT ON COLUMN back_teacher_subjects.icon_code IS
    'Code d''icône de la librairie matières (math, alphabet…). Null = pas encore choisi.';
