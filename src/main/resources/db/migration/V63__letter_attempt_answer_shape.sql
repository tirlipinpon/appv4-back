-- Lettre par lettre : une réponse de mot n'a ni bool ni choice_id.
ALTER TABLE back_child_attempt_answers
    DROP CONSTRAINT ck_back_child_attempt_answers_shape;

ALTER TABLE back_child_attempt_answers
    ADD CONSTRAINT ck_back_child_attempt_answers_shape CHECK (
        answer_bool IS NOT NULL
        OR answer_choice_id IS NOT NULL
        OR target_kind IN ('pair', 'letter')
    );

COMMENT ON TABLE back_child_attempt_answers IS
    'Détail des réponses d''une tentative (statement / choice / pair / letter / zone…).';
