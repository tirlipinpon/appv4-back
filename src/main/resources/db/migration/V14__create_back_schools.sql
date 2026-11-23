-- SCRUM-163 : annuaire d'exemple, pas l'annuaire complet.
-- Les trois lignes viennent de Export_Recherche_Annuaire.xlsx, recopiées ici.
-- Le fichier n'a pas de pays : on ajoute Belgique. On ne réécrit pas l'orthographe.
-- V13 a déjà créé les rattachements professeur. On ne modifie pas V1 à V13.

CREATE TABLE back_schools (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    country         VARCHAR(80) NOT NULL,
    teaching_types  VARCHAR(255),
    network         VARCHAR(120),
    street          VARCHAR(255),
    postal_code     VARCHAR(32),
    phone           VARCHAR(64),
    city            VARCHAR(120),
    province        VARCHAR(120),
    denomination    VARCHAR(120),
    CONSTRAINT uq_back_schools_name_street_postal UNIQUE (name, street, postal_code),
    CONSTRAINT ck_back_schools_name_present CHECK (btrim(name) <> ''),
    CONSTRAINT ck_back_schools_country_present CHECK (btrim(country) <> '')
);

COMMENT ON TABLE back_schools IS
    'Annuaire d''exemple (trois écoles), pas l''annuaire complet. Le pays est renseigné à part : il n''est pas dans le fichier, et ce n''est pas la norme des années scolaires.';

COMMENT ON COLUMN back_schools.country IS
    'Pays de l''école, obligatoire. Pour cet exemple : Belgique. Ne choisit pas la liste des classes (France ou Belgique francophone).';

COMMENT ON COLUMN back_schools.denomination IS
    'Confession. Vide dans le fichier d''exemple : NULL, pas une valeur par défaut.';

INSERT INTO back_schools (
    name, country, network, teaching_types, street, postal_code, city, province, phone, denomination
) VALUES
(
    'Ecole Nouvelle',
    'Belgique',
    'Officiel subventionné',
    'Maternelle ordinaire, Primaire ordinaire',
    'Rue de l''Hôtel des Monnaies 128B',
    '1060',
    'Saint-Gilles',
    'Bruxelles-capitale',
    '02-536.03.64',
    NULL
),
(
    'Ecole nouvelle fondamentale libre non confessionnelle Amélie Hamaide',
    'Belgique',
    'Libre non confessionnel',
    'Maternelle ordinaire, Primaire ordinaire',
    'Avenue Hamoir 31',
    '1180',
    'Uccle',
    'Bruxelles-capitale',
    '02-374 78 90',
    NULL
),
(
    'Ecole communale "La Nouvelle Ecole"',
    'Belgique',
    'Officiel subventionné',
    'Maternelle ordinaire, Primaire ordinaire',
    'Place Saint-Josse 12',
    '1210',
    'Saint-Josse-ten-Noode',
    'Bruxelles-capitale',
    '02-231 58 05',
    NULL
);

-- Le professeur pointe déjà vers un id d'école (SCRUM-159). On peut maintenant le relier.
ALTER TABLE back_teacher_schools
    ADD CONSTRAINT fk_back_teacher_schools_school
    FOREIGN KEY (school_id) REFERENCES back_schools (id);
