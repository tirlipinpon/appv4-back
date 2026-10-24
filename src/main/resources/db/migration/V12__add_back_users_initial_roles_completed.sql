-- Issue #35 : distinguer onboarding terminé des rôles encore actifs.
-- Après révocation teacher-only, les espaces sont vides mais le choix initial a déjà été fait.

ALTER TABLE back_users
    ADD COLUMN initial_roles_completed BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN back_users.initial_roles_completed IS
    'Vrai dès que le premier choix parent/professeur a été enregistré (immuable).';

-- Comptes déjà passés par complete-roles / une demande professeur (y compris révoquée).
UPDATE back_users u
SET initial_roles_completed = TRUE
WHERE EXISTS (
        SELECT 1
        FROM back_user_roles r
        WHERE r.user_id = u.id
          AND r.role IN ('parent', 'teacher')
    )
   OR EXISTS (
        SELECT 1
        FROM back_teacher_role_requests t
        WHERE t.user_id = u.id
    );
