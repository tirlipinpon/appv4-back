-- Réponses d'humeur (BACK-69). La copie ne suit pas un choix modifié ou retiré.

CREATE TABLE back_humeur_answers (
    id           BIGSERIAL PRIMARY KEY,
    child_id     BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    choice_id    BIGINT,
    icon         TEXT,
    word         TEXT,
    closed       BOOLEAN NOT NULL,
    answered_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_back_humeur_answers_shape CHECK (
        (
            closed
            AND choice_id IS NULL
            AND icon IS NULL
            AND word IS NULL
        )
        OR (
            NOT closed
            AND choice_id IS NOT NULL
            AND (
                (icon IS NOT NULL AND btrim(icon) <> '')
                OR (word IS NOT NULL AND btrim(word) <> '')
            )
        )
    )
);

CREATE INDEX idx_back_humeur_answers_child_time
    ON back_humeur_answers (child_id, answered_at DESC, id DESC);

COMMENT ON TABLE back_humeur_answers IS
    'Réponse d''humeur d''un enfant. closed : fermeture sans choix. Sinon copie du mot et de l''icône (BACK-69).';
