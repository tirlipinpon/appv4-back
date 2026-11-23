-- Personnage composé de l'enfant (BACK-48). La lecture sans ligne reste Lorelei.
CREATE TABLE back_child_avatars (
    child_id    BIGINT PRIMARY KEY REFERENCES back_children (id) ON DELETE CASCADE,
    style_code  VARCHAR(64) NOT NULL,
    preset_id   VARCHAR(64),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE back_child_avatars IS
    'Style et palette enregistrés pour un enfant. Pas de ligne : Lorelei sans palette (BACK-48).';

CREATE TABLE back_child_avatar_pins (
    child_id      BIGINT NOT NULL REFERENCES back_child_avatars (child_id) ON DELETE CASCADE,
    zone_code     VARCHAR(64) NOT NULL,
    variant_code  VARCHAR(64) NOT NULL,
    PRIMARY KEY (child_id, zone_code)
);

COMMENT ON TABLE back_child_avatar_pins IS
    'Variante choisie sur une zone ouverte. Pas de ligne : la graine dessine la zone (BACK-48).';
