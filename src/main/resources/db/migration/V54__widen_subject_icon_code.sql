-- Codes Iconify du type "prefix:name" (ex. mdi:book-open) dépassent VARCHAR(32).
ALTER TABLE back_teacher_subjects
    ALTER COLUMN icon_code TYPE VARCHAR(128);

COMMENT ON COLUMN back_teacher_subjects.icon_code IS
    'Code icône matière : legacy court (math, books…) ou Iconify prefix:name ; null si absent.';
