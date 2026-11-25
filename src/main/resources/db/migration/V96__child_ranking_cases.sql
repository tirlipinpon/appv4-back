-- BACK-22 : où l'enfant apparaît dans les classements. L'absence de ligne vaut six cases cochées.
CREATE TABLE back_child_ranking_cases (
    child_id     BIGINT PRIMARY KEY REFERENCES back_children (id) ON DELETE CASCADE,
    show_classe  BOOLEAN NOT NULL DEFAULT TRUE,
    show_annee   BOOLEAN NOT NULL DEFAULT TRUE,
    show_pays    BOOLEAN NOT NULL DEFAULT TRUE,
    show_tous    BOOLEAN NOT NULL DEFAULT TRUE,
    show_age     BOOLEAN NOT NULL DEFAULT TRUE,
    show_amitie  BOOLEAN NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE back_child_ranking_cases IS
    'Cases du parent : classe, année, pays, tout le monde, âge, et amitié gardée pour plus tard (BACK-22).';
