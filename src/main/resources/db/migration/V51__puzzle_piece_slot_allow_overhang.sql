-- Emplacement blanc : le coin haut-gauche peut légèrement sortir de [0, 1]
-- (ex. pièce déjà collée au bord gauche de la photo) tant qu'elle reste visible.
ALTER TABLE back_subject_question_puzzle_pieces
    DROP CONSTRAINT ck_back_subject_question_puzzle_pieces_slot_range;

ALTER TABLE back_subject_question_puzzle_pieces
    ADD CONSTRAINT ck_back_subject_question_puzzle_pieces_slot_range
        CHECK (
            slot_x IS NULL
            OR (slot_x >= -1 AND slot_x <= 1 AND slot_y >= -1 AND slot_y <= 1)
        );

COMMENT ON COLUMN back_subject_question_puzzle_pieces.slot_x IS
    'Coin haut-gauche X de l''emplacement (cadre blanc), peut déborder un peu hors [0,1].';

COMMENT ON COLUMN back_subject_question_puzzle_pieces.slot_y IS
    'Coin haut-gauche Y de l''emplacement (cadre blanc), peut déborder un peu hors [0,1].';
