-- SCRUM-156 : désactiver et retirer de l'affichage ne sont pas le même geste.
-- active = false : l'enfant reste dans la liste, il n'est plus proposé ailleurs.
-- hidden = true : il disparaît de l'application. La ligne, le hash et la photo restent.

ALTER TABLE back_children
    ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE;

-- L'ancienne désactivation retirait l'enfant de la liste. Ce geste s'appelle maintenant hidden.
UPDATE back_children
SET hidden = TRUE,
    active = TRUE
WHERE active = FALSE;

COMMENT ON COLUMN back_children.active IS
    'Faux : l''enfant reste affiché dans la liste du parent, mais n''est plus proposé dans le reste de l''application.';

COMMENT ON COLUMN back_children.hidden IS
    'Vrai : l''enfant n''est plus affiché. La fiche n''est pas effacée.';
