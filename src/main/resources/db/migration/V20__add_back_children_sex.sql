-- Sexe de l'enfant : le mot du lien (frère, sœur, cousin…) suit cette fiche, pas l'autre.

ALTER TABLE back_children
    ADD COLUMN sex VARCHAR(16) NOT NULL DEFAULT 'NON_GENRE';

ALTER TABLE back_children
    ADD CONSTRAINT ck_back_children_sex CHECK (sex IN ('GARCON', 'FILLE', 'NON_GENRE'));

COMMENT ON COLUMN back_children.sex IS
    'GARCON, FILLE ou NON_GENRE. Les fiches déjà là restent non genrées tant que le parent ne choisit pas.';
