-- Fond dédié : le professeur pose chaque pièce sur le fond et règle sa taille,
-- comme en cadre blanc. slot_width = largeur de la pièce sur le plateau
-- (proportion 0–1 de la largeur du fond). NULL = taille de la découpe.

ALTER TABLE back_subject_question_puzzle_pieces
    ADD COLUMN slot_width DOUBLE PRECISION;

ALTER TABLE back_subject_question_puzzle_pieces
    ADD CONSTRAINT ck_back_subject_question_puzzle_pieces_slot_width
        CHECK (
            slot_width IS NULL
            OR (slot_x IS NOT NULL AND slot_width > 0 AND slot_width <= 1)
        );

COMMENT ON COLUMN back_subject_question_puzzle_pieces.slot_x IS
    'Coin haut-gauche X de l''emplacement (cadre blanc ou fond dédié), peut déborder un peu hors [0,1].';

COMMENT ON COLUMN back_subject_question_puzzle_pieces.slot_y IS
    'Coin haut-gauche Y de l''emplacement (cadre blanc ou fond dédié), peut déborder un peu hors [0,1].';

COMMENT ON COLUMN back_subject_question_puzzle_pieces.slot_width IS
    'Largeur de la pièce sur le fond dédié (0–1 de la largeur du plateau). NULL = taille de la découpe.';
