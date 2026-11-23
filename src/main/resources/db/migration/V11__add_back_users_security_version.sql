-- Issue #49 : invalider les JWT déjà émis (révocation rôle, reset mdp, désactivation).
-- Claim JWT « sv » = cette colonne ; filtre refuse si décalage.

ALTER TABLE back_users
    ADD COLUMN security_version INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN back_users.security_version IS
    'Incrémenté pour invalider les sessions JWT (révocation, reset mdp, etc.).';
