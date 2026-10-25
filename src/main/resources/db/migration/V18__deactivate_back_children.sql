-- SCRUM-156 : retirer un enfant le désactive. La ligne, le hash du PIN et la photo restent.

ALTER TABLE back_children
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN back_children.active IS
    'Vrai tant que le parent voit l''enfant. Faux après une désactivation : la fiche n''est plus proposée, elle n''est pas effacée.';
