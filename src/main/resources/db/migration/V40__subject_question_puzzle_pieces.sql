-- SCRUM-183 : puzzle préparé par le professeur.
-- L'image stockée est déjà un WebP allégé. Les pièces sont en proportion de cette image.
-- Pas de colonne correct : ce n'est pas la question clic. L'élève ne joue pas encore.

CREATE TABLE back_subject_question_puzzle_images (
    question_id  BIGINT PRIMARY KEY REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    image_webp   BYTEA NOT NULL,
    CONSTRAINT ck_back_subject_question_puzzle_images_bytes CHECK (octet_length(image_webp) > 0)
);

COMMENT ON TABLE back_subject_question_puzzle_images IS
    'Image WebP d''un puzzle. Le fichier d''origine déposé par le professeur n''est pas gardé.';

COMMENT ON COLUMN back_subject_question_puzzle_images.image_webp IS
    'Fichier WebP déjà réduit.';

CREATE TABLE back_subject_question_puzzle_pieces (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    label        VARCHAR(200) NOT NULL,
    shape        VARCHAR(16) NOT NULL,
    points       VARCHAR(800) NOT NULL,
    sort_order   INTEGER NOT NULL,
    CONSTRAINT ck_back_subject_question_puzzle_pieces_label CHECK (char_length(btrim(label)) > 0),
    CONSTRAINT ck_back_subject_question_puzzle_pieces_shape CHECK (shape IN ('polygon', 'rect', 'ellipse')),
    CONSTRAINT ck_back_subject_question_puzzle_pieces_points CHECK (char_length(points) > 0),
    CONSTRAINT ck_back_subject_question_puzzle_pieces_position CHECK (sort_order >= 0),
    CONSTRAINT uq_back_subject_question_puzzle_pieces_position UNIQUE (question_id, sort_order)
);

COMMENT ON TABLE back_subject_question_puzzle_pieces IS
    'Pièce découpée dans l''image. Les points sont entre 0 et 1. Pas de vert ni de rouge.';

COMMENT ON COLUMN back_subject_question_puzzle_pieces.points IS
    'Points en proportion de l''image, séparés par un point-virgule. Un rectangle et une ellipse en ont deux.';

COMMENT ON COLUMN back_subject_question_puzzle_pieces.shape IS
    'polygon : tracé libre. rect : rectangle. ellipse : ellipse.';
