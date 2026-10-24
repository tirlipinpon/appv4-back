-- SCRUM-156 : fiches enfant d'un parent. Le PIN est un hash, jamais le code en clair.
-- L'école est facultative : la liste existe, mais l'enfant peut être enregistré sans.

CREATE TABLE back_children (
    id                  BIGSERIAL PRIMARY KEY,
    parent_user_id      BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    age                 SMALLINT NOT NULL,
    curriculum          VARCHAR(32) NOT NULL,
    grade_label         VARCHAR(80) NOT NULL,
    school_id           BIGINT REFERENCES back_schools (id),
    avatar_code         VARCHAR(32),
    photo               BYTEA,
    photo_content_type  VARCHAR(40),
    pin_hash            VARCHAR(255) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_back_children_age CHECK (age BETWEEN 2 AND 18),
    CONSTRAINT ck_back_children_curriculum CHECK (curriculum IN ('FRANCE', 'BELGIUM_FR')),
    CONSTRAINT ck_back_children_names CHECK (btrim(first_name) <> '' AND btrim(last_name) <> ''),
    CONSTRAINT ck_back_children_one_image CHECK (
        (avatar_code IS NOT NULL AND photo IS NULL)
        OR (avatar_code IS NULL AND photo IS NOT NULL)
    )
);

CREATE INDEX idx_back_children_parent ON back_children (parent_user_id);

COMMENT ON TABLE back_children IS
    'Enfant inscrit par un parent : prénom, nom, âge, classe, école facultative, photo ou avatar, et hash du code PIN. Le PIN ne sert pas encore à ouvrir une session.';

COMMENT ON COLUMN back_children.pin_hash IS
    'Hash du code à 4 chiffres. Jamais le PIN en clair.';

COMMENT ON COLUMN back_children.school_id IS
    'École de l''annuaire, ou vide si le parent n''en a pas choisi.';
