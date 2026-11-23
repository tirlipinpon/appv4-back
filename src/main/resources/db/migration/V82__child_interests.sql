-- Centres d'intérêt d'un enfant (BACK-72). Une copie par mot, avec son moment.

CREATE TABLE back_child_interests (
    id          BIGSERIAL PRIMARY KEY,
    child_id    BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    label       VARCHAR(40) NOT NULL,
    written_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_back_child_interests_label CHECK (char_length(label) BETWEEN 2 AND 40)
);

CREATE INDEX idx_back_child_interests_child ON back_child_interests (child_id);

COMMENT ON TABLE back_child_interests IS
    'Centre d''intérêt écrit par le parent sur la fiche. label est l''orthographe affichée. written_at est le moment de la copie.';

COMMENT ON COLUMN back_child_interests.label IS
    'Orthographe gardée, déjà réduite. La forme comparée ne s''enregistre pas.';

COMMENT ON COLUMN back_child_interests.written_at IS
    'Moment de la copie. Une correction de la même forme le conserve.';
