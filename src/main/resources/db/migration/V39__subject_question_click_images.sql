-- SCRUM-181 : image à cliquer préparée par le professeur.
-- L'image stockée est déjà un WebP allégé. Les zones sont en proportion de cette image.
-- L'élève ne joue pas encore.

CREATE TABLE back_subject_question_click_images (
    question_id          BIGINT PRIMARY KEY REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    image_webp           BYTEA NOT NULL,
    require_all_correct  BOOLEAN NOT NULL,
    CONSTRAINT ck_back_subject_question_click_images_bytes CHECK (octet_length(image_webp) > 0)
);

COMMENT ON TABLE back_subject_question_click_images IS
    'Image WebP d''une question à cliquer, et si toutes les zones correctes doivent être cliquées.';

COMMENT ON COLUMN back_subject_question_click_images.image_webp IS
    'Fichier WebP déjà réduit. Pas le fichier d''origine déposé par le professeur.';

COMMENT ON COLUMN back_subject_question_click_images.require_all_correct IS
    'Vrai : l''élève devra cliquer chaque zone correcte. Faux : une partie suffit.';

CREATE TABLE back_subject_question_click_zones (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES back_subject_questions (id) ON DELETE CASCADE,
    label        VARCHAR(200) NOT NULL,
    correct      BOOLEAN NOT NULL,
    shape        VARCHAR(16) NOT NULL,
    points       VARCHAR(800) NOT NULL,
    sort_order   INTEGER NOT NULL,
    CONSTRAINT ck_back_subject_question_click_zones_label CHECK (char_length(btrim(label)) > 0),
    CONSTRAINT ck_back_subject_question_click_zones_shape CHECK (shape IN ('polygon', 'rect', 'ellipse')),
    CONSTRAINT ck_back_subject_question_click_zones_points CHECK (char_length(points) > 0),
    CONSTRAINT ck_back_subject_question_click_zones_position CHECK (sort_order >= 0),
    CONSTRAINT uq_back_subject_question_click_zones_position UNIQUE (question_id, sort_order)
);

COMMENT ON TABLE back_subject_question_click_zones IS
    'Zone dessinée sur l''image. correct distingue le vert du rouge chez le professeur. Les points sont entre 0 et 1.';

COMMENT ON COLUMN back_subject_question_click_zones.points IS
    'Points en proportion de l''image, séparés par un point-virgule. Un rectangle et une ellipse en ont deux.';

COMMENT ON COLUMN back_subject_question_click_zones.shape IS
    'polygon : tracé libre. rect : rectangle. ellipse : ellipse.';
