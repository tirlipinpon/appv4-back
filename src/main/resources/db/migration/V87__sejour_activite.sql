-- Séjour de l'enfant (BACK-38).
-- Le temps fermé est un intervalle. La tranche ouverte reste sur le séjour :
-- début, dernier signal confirmé, horloge du geste. Pas une ligne par seconde.
-- Un enfant a au plus un séjour ouvert. Le jeu de démo n'insère aucun séjour.

CREATE TABLE back_sejours (
    id                 BIGSERIAL PRIMARY KEY,
    child_id           BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    started_at         TIMESTAMPTZ NOT NULL,
    closed_at          TIMESTAMPTZ,
    holding_device_id  TEXT,
    slice_screen       TEXT,
    slice_started_at   TIMESTAMPTZ,
    last_confirmed_at  TIMESTAMPTZ,
    last_gesture_at    TIMESTAMPTZ,
    CONSTRAINT ck_back_sejours_screen CHECK (
        slice_screen IS NULL
        OR slice_screen IN (
            'accueil', 'matieres', 'board', 'questions', 'recompenses', 'classement', 'profil')
    ),
    CONSTRAINT ck_back_sejours_shape CHECK (
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
            AND slice_started_at IS NULL
            AND last_confirmed_at IS NULL
            AND last_gesture_at IS NULL
        )
    )
);

CREATE UNIQUE INDEX uq_back_sejours_one_open
    ON back_sejours (child_id)
    WHERE closed_at IS NULL;

CREATE INDEX idx_back_sejours_child_start
    ON back_sejours (child_id, started_at, id);

CREATE TABLE back_sejour_intervals (
    id          BIGSERIAL PRIMARY KEY,
    sejour_id   BIGINT NOT NULL REFERENCES back_sejours (id) ON DELETE CASCADE,
    screen      TEXT NOT NULL,
    started_at  TIMESTAMPTZ NOT NULL,
    ended_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_back_sejour_intervals_screen CHECK (
        screen IN ('accueil', 'matieres', 'board', 'questions', 'recompenses', 'classement', 'profil')
    ),
    CONSTRAINT ck_back_sejour_intervals_span CHECK (ended_at > started_at)
);

CREATE INDEX idx_back_sejour_intervals_sejour
    ON back_sejour_intervals (sejour_id, started_at, id);

COMMENT ON TABLE back_sejours IS
    'Passage d''un enfant. Ouvert : un appareil tient la tranche. Fermé : seuls les intervalles restent (BACK-38).';

COMMENT ON TABLE back_sejour_intervals IS
    'Morceau de séjour déjà fermé, d''un écran, du début confirmé à la fin confirmée. Pas une seconde par ligne.';

COMMENT ON COLUMN back_sejours.holding_device_id IS
    'Identifiant opaque de l''appareil qui tient le séjour ouvert. Vide une fois le séjour fermé.';

COMMENT ON COLUMN back_sejours.last_gesture_at IS
    'Dernier geste de la tranche ouverte. L''activité confirmée ne dépasse pas cet instant plus deux minutes.';

COMMENT ON COLUMN back_sejours.last_confirmed_at IS
    'Dernier signal qui a confirmé la tranche ouverte. Le temps d''après n''est pas compté.';
