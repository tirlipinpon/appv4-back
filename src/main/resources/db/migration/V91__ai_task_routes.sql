-- BACK-30 : la route d'un besoin vers le fournisseur et le modèle du moment.
-- Vide au départ : le démarrage pose les trois besoins de la config.

CREATE TABLE back_ai_task_routes (
    code VARCHAR(64) PRIMARY KEY,
    label VARCHAR(120) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE back_ai_task_routes IS
    'Route d''un besoin IA vers un fournisseur et un modèle. active faux : l''appel est refusé.';
