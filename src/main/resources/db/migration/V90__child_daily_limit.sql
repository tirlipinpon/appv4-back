-- BACK-42 : plafond de la journée, en minutes, sur la fiche enfant.
-- Vide : pas de limite. 1 à 960 : les heures hors Nuit, de 6 h à 22 h.

ALTER TABLE back_children
    ADD COLUMN daily_limit_minutes INTEGER;

ALTER TABLE back_children
    ADD CONSTRAINT ck_back_children_daily_limit
    CHECK (daily_limit_minutes IS NULL OR daily_limit_minutes BETWEEN 1 AND 960);

COMMENT ON COLUMN back_children.daily_limit_minutes IS
    'Plafond de la journée en minutes, posé par le parent référent. Vide : pas de plafond. La Nuit, de 22 h à 6 h, n''est pas comprise dans ces minutes.';
