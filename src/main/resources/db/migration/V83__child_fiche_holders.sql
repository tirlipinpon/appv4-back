-- Détenteurs d'une fiche et invitations (BACK-74).
-- Le parent_user_id historique reste le parent qui a inscrit l'enfant.
-- Un détenteur en plus le voit et l'enregistre. Lui seul ne le retire pas.

CREATE TABLE back_child_holders (
    child_id    BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    referent    BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (child_id, user_id)
);

CREATE UNIQUE INDEX uq_back_child_holders_referent
    ON back_child_holders (child_id)
    WHERE referent;

COMMENT ON TABLE back_child_holders IS
    'Parents qui ont la fiche. Un seul peut être le parent référent.';

CREATE TABLE back_child_invitations (
    id          BIGSERIAL PRIMARY KEY,
    child_id    BIGINT NOT NULL REFERENCES back_children (id) ON DELETE CASCADE,
    email       VARCHAR(320) NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'pending',
    CONSTRAINT ck_back_child_invitations_status CHECK (status IN ('pending', 'accepted', 'cancelled')),
    CONSTRAINT ck_back_child_invitations_email CHECK (char_length(btrim(email)) > 0)
);

CREATE UNIQUE INDEX uq_back_child_invitations_pending
    ON back_child_invitations (child_id, email)
    WHERE status = 'pending';

COMMENT ON TABLE back_child_invitations IS
    'Invitation à une fiche, adressée à un email, sans dire si le compte existe.';
