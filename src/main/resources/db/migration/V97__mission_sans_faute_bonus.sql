-- Bonus d'XP figé quand la mission du jour se termine.
-- Vide : mission pas encore finie, ou antérieure à cette règle.
-- 0 : finie avec un essai raté. Positif : sans faute, déjà ajouté aux XP.

ALTER TABLE back_child_daily_missions
    ADD COLUMN bonus_xp INTEGER;

ALTER TABLE back_child_daily_missions
    ADD CONSTRAINT ck_back_child_daily_mission_bonus
        CHECK (bonus_xp IS NULL OR bonus_xp >= 0);

COMMENT ON COLUMN back_child_daily_missions.bonus_xp IS
    'XP ajoutées pour une mission sans faute. 0 si une erreur a retiré le bonus. Vide sinon (V97).';
