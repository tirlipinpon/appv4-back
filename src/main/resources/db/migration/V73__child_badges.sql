-- Badges enfant (BACK-19). pay mémorise la décision déjà prise pour l'XP.
ALTER TABLE back_child_attempts ADD COLUMN pay BOOLEAN;

COMMENT ON COLUMN back_child_attempts.pay IS
    'Décision pay de l''XP pour cet essai. Null : essai ancien, ignoré par les badges qui suivent pay.';

CREATE TABLE back_child_badges (
    child_id        BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    code            VARCHAR(40) NOT NULL,
    level           INTEGER NOT NULL,
    retained_value  INTEGER NOT NULL,
    unlocked_on     DATE,
    PRIMARY KEY (child_id, code),
    CONSTRAINT ck_back_child_badges_level CHECK (level >= 0),
    CONSTRAINT ck_back_child_badges_retained CHECK (retained_value >= 0)
);

COMMENT ON TABLE back_child_badges IS
    'Niveau déjà atteint d''un badge pour un enfant. Pas de ligne : niveau 0 (BACK-19).';

COMMENT ON COLUMN back_child_badges.unlocked_on IS
    'Jour Europe/Paris du franchissement. Null si la reprise n''a pas pu dater l''événement.';

CREATE TABLE back_child_badge_inits (
    child_id BIGINT PRIMARY KEY REFERENCES back_children (id) ON DELETE CASCADE
);

COMMENT ON TABLE back_child_badge_inits IS
    'L''enfant a déjà eu sa reprise des badges, avant le prochain essai (BACK-19).';
