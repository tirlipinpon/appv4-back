-- Cadre blanc : le professeur pose chaque pièce sur le plateau pour fixer
-- l'emplacement cible (coin haut-gauche, proportion 0–1). Hors mode blanc : NULL.

ALTER TABLE back_subject_question_puzzle_pieces
    ADD COLUMN slot_x DOUBLE PRECISION;

ALTER TABLE back_subject_question_puzzle_pieces
    ADD COLUMN slot_y DOUBLE PRECISION;

ALTER TABLE back_subject_question_puzzle_pieces
    ADD CONSTRAINT ck_back_subject_question_puzzle_pieces_slot_pair
        CHECK ((slot_x IS NULL) = (slot_y IS NULL));

ALTER TABLE back_subject_question_puzzle_pieces
    ADD CONSTRAINT ck_back_subject_question_puzzle_pieces_slot_range
        CHECK (
            slot_x IS NULL
            OR (slot_x >= 0 AND slot_x <= 1 AND slot_y >= 0 AND slot_y <= 1)
        );

COMMENT ON COLUMN back_subject_question_puzzle_pieces.slot_x IS
    'Emplacement cible sur le plateau (X, 0–1), obligatoire en mode blanc.';

COMMENT ON COLUMN back_subject_question_puzzle_pieces.slot_y IS
    'Emplacement cible sur le plateau (Y, 0–1), obligatoire en mode blanc.';
