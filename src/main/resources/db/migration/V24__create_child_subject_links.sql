-- SCRUM-162 : un enfant suit des matières déjà créées par des professeurs.
-- Retirer le lien n'efface pas la matière. Une matière cachée ou désactivée
-- n'est plus montrée au parent, mais le lien peut rester.

CREATE TABLE back_child_subject_links (
    id          BIGSERIAL PRIMARY KEY,
    child_id    BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    subject_id  BIGINT NOT NULL REFERENCES back_teacher_subjects (id) ON DELETE CASCADE,
    CONSTRAINT uq_back_child_subject_links_child_subject UNIQUE (child_id, subject_id)
);

CREATE INDEX idx_back_child_subject_links_child ON back_child_subject_links (child_id);

COMMENT ON TABLE back_child_subject_links IS
    'Matière ajoutée à un enfant par son parent. La ligne de la matière du professeur n''est pas effacée quand ce lien part.';
