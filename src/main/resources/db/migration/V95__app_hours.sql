-- Horaires de l'app enfant. Une seule ligne, la même pour tous les enfants.
-- Au départ : ouverte à 6 h, fermée à 22 h, heure de la progression.

CREATE TABLE back_app_hours (
    id        SMALLINT PRIMARY KEY,
    opens_at  TIME NOT NULL,
    closes_at TIME NOT NULL,
    CONSTRAINT ck_back_app_hours_singleton CHECK (id = 1),
    CONSTRAINT ck_back_app_hours_window CHECK (opens_at < closes_at)
);

COMMENT ON TABLE back_app_hours IS
    'Heure de début et heure de fin de l''app enfant. L''admin les change. Une seule ligne.';

COMMENT ON COLUMN back_app_hours.opens_at IS
    'Heure de début, incluse. Avant cette heure, l''app est fermée.';

COMMENT ON COLUMN back_app_hours.closes_at IS
    'Heure de fin. À partir de cette heure, l''app est fermée jusqu''au début suivant.';

INSERT INTO back_app_hours (id, opens_at, closes_at)
VALUES (1, TIME '06:00', TIME '22:00');
