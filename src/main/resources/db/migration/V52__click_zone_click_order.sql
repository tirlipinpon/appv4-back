-- Image à cliquer : l'ordre dans lequel les zones correctes doivent être cliquées.
-- click_order NULL = zone incorrecte. Sinon 1, puis 2, puis 3, sans doublon.
-- Les questions déjà enregistrées reçoivent cet ordre d'après leur position actuelle.

ALTER TABLE back_subject_question_click_zones
    ADD COLUMN click_order INTEGER;

UPDATE back_subject_question_click_zones AS zone
SET click_order = ranked.step
FROM (
    SELECT id,
           row_number() OVER (PARTITION BY question_id ORDER BY sort_order, id) AS step
    FROM back_subject_question_click_zones
    WHERE correct
) AS ranked
WHERE zone.id = ranked.id;

ALTER TABLE back_subject_question_click_zones
    ADD CONSTRAINT ck_back_subject_question_click_zones_click_order
        CHECK (
            (correct AND click_order IS NOT NULL AND click_order >= 1)
            OR (NOT correct AND click_order IS NULL)
        );

CREATE UNIQUE INDEX uq_back_subject_question_click_zones_click_order
    ON back_subject_question_click_zones (question_id, click_order)
    WHERE click_order IS NOT NULL;

COMMENT ON COLUMN back_subject_question_click_zones.click_order IS
    'Étape de clic parmi les zones correctes : 1, puis 2, puis 3. NULL si la zone est incorrecte.';
