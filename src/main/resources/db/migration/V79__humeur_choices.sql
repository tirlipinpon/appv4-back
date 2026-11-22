-- Bibliothèque unique des humeurs (BACK-68). Pas de fichier, pas de catalogue.

CREATE TABLE back_humeur_choices (
    id          BIGSERIAL PRIMARY KEY,
    icon        TEXT,
    word        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_back_humeur_choices_present CHECK (
        (icon IS NOT NULL AND btrim(icon) <> '')
        OR (word IS NOT NULL AND btrim(word) <> '')
    )
);

COMMENT ON TABLE back_humeur_choices IS
    'Choix d''humeur proposés à tous les enfants. L''ordre de lecture est l''identifiant (BACK-68).';

COMMENT ON COLUMN back_humeur_choices.icon IS
    'Texte saisi par l''admin, souvent un emoji. Null si le choix n''a qu''un mot.';

COMMENT ON COLUMN back_humeur_choices.word IS
    'Mot saisi par l''admin. Null si le choix n''a qu''une icône.';
