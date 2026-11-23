ALTER TABLE back_child_simon_challenges
    ADD COLUMN challenge_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN legacy BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE back_child_simon_challenges
    DROP CONSTRAINT pk_back_child_simon_challenges,
    ADD CONSTRAINT pk_back_child_simon_challenges PRIMARY KEY (challenge_id);

CREATE UNIQUE INDEX uq_back_child_simon_challenges_legacy
    ON back_child_simon_challenges (child_id, question_id) WHERE legacy;
CREATE INDEX idx_back_child_simon_challenges_child
    ON back_child_simon_challenges (child_id);
CREATE INDEX idx_back_child_simon_challenges_question
    ON back_child_simon_challenges (question_id);
CREATE INDEX idx_back_child_simon_challenges_expiry
    ON back_child_simon_challenges (created_at);

COMMENT ON TABLE back_child_simon_challenges IS
    'Une suite par ouverture Simon. Le défi appartient à un enfant et à une question, expire et ne sert qu''une fois.';

ALTER TABLE back_child_attempts ADD COLUMN submission_id UUID;
CREATE UNIQUE INDEX uq_back_child_attempts_submission
    ON back_child_attempts (child_id, submission_id) WHERE submission_id IS NOT NULL;

COMMENT ON COLUMN back_child_attempts.submission_id IS
    'Identifiant de relance client. Une tentative déjà enregistrée rend le même verdict sans crédit supplémentaire.';
