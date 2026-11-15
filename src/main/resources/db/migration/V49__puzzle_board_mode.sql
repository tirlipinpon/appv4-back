-- Trois plateaux de jeu pour le puzzle :
-- fond  = image de fond dédiée
-- blanc = cadre blanc
-- trous = image de découpe trouée

ALTER TABLE back_subject_question_puzzle_images
    ADD COLUMN board_mode VARCHAR(16);

UPDATE back_subject_question_puzzle_images
SET board_mode = 'trous'
WHERE board_mode IS NULL;

ALTER TABLE back_subject_question_puzzle_images
    ALTER COLUMN board_mode SET NOT NULL;

ALTER TABLE back_subject_question_puzzle_images
    ADD CONSTRAINT ck_back_subject_question_puzzle_images_board_mode
        CHECK (board_mode IN ('fond', 'blanc', 'trous'));

ALTER TABLE back_subject_question_puzzle_images
    ALTER COLUMN board_image_webp DROP NOT NULL;

ALTER TABLE back_subject_question_puzzle_images
    DROP CONSTRAINT IF EXISTS ck_back_subject_question_puzzle_images_board_bytes;

ALTER TABLE back_subject_question_puzzle_images
    ADD CONSTRAINT ck_back_subject_question_puzzle_images_board_bytes
        CHECK (board_image_webp IS NULL OR octet_length(board_image_webp) > 0);

-- Sans mode fond, le blob de fond ne sert plus.
UPDATE back_subject_question_puzzle_images
SET board_image_webp = NULL
WHERE board_mode <> 'fond';

COMMENT ON COLUMN back_subject_question_puzzle_images.board_mode IS
    'Plateau de jeu : fond (image dédiée), blanc (cadre vide), trous (découpe trouée).';

COMMENT ON COLUMN back_subject_question_puzzle_images.board_image_webp IS
    'WebP du fond de jeu, obligatoire seulement si board_mode = fond.';
