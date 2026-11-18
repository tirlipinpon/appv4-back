-- Memory : une réponse de paire n'a ni bool ni choice_id (BACK-43 suivi).
ALTER TABLE back_child_attempt_answers
    DROP CONSTRAINT ck_back_child_attempt_answers_shape;

ALTER TABLE back_child_attempt_answers
    ADD CONSTRAINT ck_back_child_attempt_answers_shape CHECK (
        answer_bool IS NOT NULL
        OR answer_choice_id IS NOT NULL
        OR target_kind = 'pair'
    );

COMMENT ON TABLE back_child_attempt_answers IS
    'Détail des réponses d''une tentative (statement / choice / pair).';
