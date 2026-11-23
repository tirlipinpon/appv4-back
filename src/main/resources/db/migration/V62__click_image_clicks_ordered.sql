-- Image à cliquer : le professeur choisit si les clics ont un ordre.
-- true (défaut) : les zones justes gardent les rangs 1, 2, 3…
-- false : aucune zone n'a de rang. L'enfant clique les bonnes zones dans n'importe quel ordre.

ALTER TABLE back_subject_question_click_images
    ADD COLUMN clicks_ordered BOOLEAN NOT NULL DEFAULT true;

COMMENT ON COLUMN back_subject_question_click_images.clicks_ordered IS
    'true : un clic juste par étape, dans l''ordre des rangs. false : les zones justes n''ont pas de rang.';

ALTER TABLE back_subject_question_click_zones
    DROP CONSTRAINT ck_back_subject_question_click_zones_click_order;

ALTER TABLE back_subject_question_click_zones
    ADD CONSTRAINT ck_back_subject_question_click_zones_click_order
        CHECK (
            (NOT correct AND click_order IS NULL)
            OR (correct AND (click_order IS NULL OR click_order >= 1))
        );

COMMENT ON COLUMN back_subject_question_click_zones.click_order IS
    'Étape de clic (1, 2, 3) si la question est ordonnée et la zone juste. NULL sinon.';
