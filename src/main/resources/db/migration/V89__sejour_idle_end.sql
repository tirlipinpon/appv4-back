-- L'oubli de vingt-cinq minutes se distingue d'un départ et de la Nuit (BACK-81).
-- Seul cet oubli attend une nouvelle entrée de jour. Les fermetures déjà en base
-- restent des départs : elles ne bloquent pas l'enfant. Le jeu de démo n'insère aucun séjour.

ALTER TABLE back_sejours
    ADD COLUMN ended_by TEXT;

UPDATE back_sejours
   SET ended_by = 'leave'
 WHERE closed_at IS NOT NULL
   AND ended_by IS NULL;

ALTER TABLE back_sejours
    ADD CONSTRAINT ck_back_sejours_ended_by CHECK (
        (closed_at IS NULL AND ended_by IS NULL)
        OR (closed_at IS NOT NULL AND ended_by IN ('leave', 'nuit', 'idle'))
    );

COMMENT ON COLUMN back_sejours.ended_by IS
    'Raison de la fermeture : leave (départ), nuit, ou idle (vingt-cinq minutes sans Je continue). Vide tant que le séjour est ouvert.';
