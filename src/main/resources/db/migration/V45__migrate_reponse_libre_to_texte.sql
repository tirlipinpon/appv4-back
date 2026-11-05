-- Ancienne forme SCRUM-182 (reponse_libre / lettres) jamais câblée en store Java :
-- le GET /questions plantait en 500 dès qu'une telle ligne existait.
-- On bascule vers la forme texte (SCRUM-198, même réponse attendue).

INSERT INTO back_subject_question_texts (question_id, expected_answer)
SELECT l.question_id, l.expected_answer
FROM back_subject_question_letters l
WHERE NOT EXISTS (
    SELECT 1
    FROM back_subject_question_texts t
    WHERE t.question_id = l.question_id
);

UPDATE back_subject_questions
SET form_code = 'texte'
WHERE form_code = 'reponse_libre';

UPDATE back_subject_question_forms AS f
SET form_code = 'texte',
    label = 'Réponse libre'
WHERE f.form_code = 'reponse_libre'
  AND NOT EXISTS (
      SELECT 1
      FROM back_subject_question_forms AS other
      WHERE other.subject_id = f.subject_id
        AND other.form_code = 'texte'
  );

DELETE FROM back_subject_question_forms
WHERE form_code = 'reponse_libre';

DELETE FROM back_subject_question_letters;
