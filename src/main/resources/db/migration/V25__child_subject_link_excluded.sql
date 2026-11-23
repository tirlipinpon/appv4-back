-- Le parent peut retirer une matière de la classe. Elle ne se rattache plus toute seule.

ALTER TABLE back_child_subject_links
    ADD COLUMN excluded BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN back_child_subject_links.excluded IS
    'Vrai : le parent a retiré cette matière de la classe. Elle reste proposable, mais n''est plus ajoutée par défaut.';
