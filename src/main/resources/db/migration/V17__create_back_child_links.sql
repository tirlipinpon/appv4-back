-- SCRUM-158 : lien entre le nouvel enfant et l'enfant dont on est parti.
-- « Aucun » n'est pas une ligne. Le PIN de l'enfant de départ n'est pas recopié.

CREATE TABLE back_child_links (
    id                 BIGSERIAL PRIMARY KEY,
    parent_user_id     BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    subject_child_id   BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    related_child_id   BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    relation           VARCHAR(32) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_back_child_links_distinct CHECK (subject_child_id <> related_child_id),
    CONSTRAINT ck_back_child_links_relation CHECK (relation IN (
        'FRERE', 'SOEUR', 'DEMI_FRERE', 'DEMI_SOEUR', 'JUMEAU', 'JUMELLE', 'COUSIN', 'COUSINE'
    )),
    CONSTRAINT uq_back_child_links_pair UNIQUE (subject_child_id, related_child_id)
);

CREATE INDEX idx_back_child_links_parent ON back_child_links (parent_user_id);
CREATE INDEX idx_back_child_links_related ON back_child_links (related_child_id);

COMMENT ON TABLE back_child_links IS
    'Lien de parenté : le sujet est le nouvel enfant, l''autre est l''enfant de départ. La phrase est reconstruite avec leurs prénoms.';

COMMENT ON COLUMN back_child_links.relation IS
    'Valeur fermée : FRERE, SOEUR, DEMI_FRERE, DEMI_SOEUR, JUMEAU, JUMELLE, COUSIN, COUSINE.';
