-- Fond de jeu du puzzle : image sur laquelle l'enfant repose les pièces.
-- Distincte de image_webp, qui sert uniquement à découper les vignettes.

ALTER TABLE back_subject_question_puzzle_images
    ADD COLUMN board_image_webp BYTEA;

UPDATE back_subject_question_puzzle_images
SET board_image_webp = image_webp
WHERE board_image_webp IS NULL;

ALTER TABLE back_subject_question_puzzle_images
    ALTER COLUMN board_image_webp SET NOT NULL;

ALTER TABLE back_subject_question_puzzle_images
    ADD CONSTRAINT ck_back_subject_question_puzzle_images_board_bytes
        CHECK (octet_length(board_image_webp) > 0);

COMMENT ON COLUMN back_subject_question_puzzle_images.board_image_webp IS
    'WebP du fond de jeu : l''enfant y glisse les pièces découpées dans image_webp.';
