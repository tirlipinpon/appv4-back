-- Le jeu compte sur la question, pas comme un écran (BACK-79).
-- L'énoncé et le nom de matière sont copiés quand la ligne atteint une seconde.
-- Pas de clé étrangère : retirer ou renommer la question ne change pas cette copie.
-- Le jeu de démo n'insère toujours aucun séjour.

ALTER TABLE back_sejours
    ADD COLUMN slice_question_id BIGINT,
    ADD COLUMN slice_prompt TEXT,
    ADD COLUMN slice_subject_name TEXT;

ALTER TABLE back_sejours DROP CONSTRAINT ck_back_sejours_shape;

ALTER TABLE back_sejours ADD CONSTRAINT ck_back_sejours_shape CHECK (
    (
        closed_at IS NULL
        AND holding_device_id IS NOT NULL
        AND btrim(holding_device_id) <> ''
        AND slice_started_at IS NOT NULL
        AND last_confirmed_at IS NOT NULL
    )
    OR (
        closed_at IS NOT NULL
        AND holding_device_id IS NULL
        AND slice_screen IS NULL
        AND slice_question_id IS NULL
        AND slice_prompt IS NULL
        AND slice_subject_name IS NULL
        AND slice_started_at IS NULL
        AND last_confirmed_at IS NULL
        AND last_gesture_at IS NULL
    )
);

ALTER TABLE back_sejours ADD CONSTRAINT ck_back_sejours_question CHECK (
    slice_question_id IS NULL
    OR (slice_screen IS NULL AND slice_question_id > 0)
);

ALTER TABLE back_sejours ADD CONSTRAINT ck_back_sejours_question_copy CHECK (
    (slice_prompt IS NULL AND slice_subject_name IS NULL)
    OR (
        slice_question_id IS NOT NULL
        AND slice_prompt IS NOT NULL
        AND slice_subject_name IS NOT NULL
    )
);

COMMENT ON COLUMN back_sejours.slice_question_id IS
    'Question de la tranche ouverte. Vide sur un écran, et une fois le séjour fermé.';

COMMENT ON COLUMN back_sejours.slice_prompt IS
    'Énoncé copié quand cette question a atteint une seconde. Un renommage ultérieur ne le change pas.';

COMMENT ON COLUMN back_sejours.slice_subject_name IS
    'Nom de matière copié en même temps que l''énoncé.';

ALTER TABLE back_sejour_intervals
    ALTER COLUMN screen DROP NOT NULL,
    ADD COLUMN question_id BIGINT,
    ADD COLUMN prompt TEXT,
    ADD COLUMN subject_name TEXT;

ALTER TABLE back_sejour_intervals DROP CONSTRAINT ck_back_sejour_intervals_screen;

ALTER TABLE back_sejour_intervals ADD CONSTRAINT ck_back_sejour_intervals_place CHECK (
    (
        question_id IS NULL
        AND prompt IS NULL
        AND subject_name IS NULL
        AND screen IN (
            'accueil', 'matieres', 'board', 'questions', 'recompenses', 'classement', 'profil')
    )
    OR (
        screen IS NULL
        AND question_id IS NOT NULL
        AND question_id > 0
        AND (
            (prompt IS NULL AND subject_name IS NULL)
            OR (prompt IS NOT NULL AND subject_name IS NOT NULL)
        )
    )
);

COMMENT ON COLUMN back_sejour_intervals.question_id IS
    'Question de ce morceau fermé. Vide pour un écran. Deux visites de la même question restent une ligne.';

COMMENT ON COLUMN back_sejour_intervals.prompt IS
    'Énoncé copié au moment où la ligne a atteint une seconde. Vide si ce morceau est resté sous une seconde.';

COMMENT ON COLUMN back_sejour_intervals.subject_name IS
    'Nom de matière copié avec l''énoncé.';
