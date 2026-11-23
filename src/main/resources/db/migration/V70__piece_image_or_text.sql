-- BACK-55 : une pièce est un texte ou une image. Un élément Simon est une image,
-- une couleur sans mot, ou un texte libre. Les octets restent dans Postgres.

ALTER TABLE back_subject_question_choices
    ALTER COLUMN label DROP NOT NULL,
    ADD COLUMN image_webp BYTEA;
ALTER TABLE back_subject_question_choices DROP CONSTRAINT ck_back_subject_question_choices_label;
ALTER TABLE back_subject_question_choices
    ADD CONSTRAINT ck_back_subject_question_choices_face CHECK (
        (image_webp IS NULL AND label IS NOT NULL AND char_length(btrim(label)) > 0)
        OR (image_webp IS NOT NULL AND octet_length(image_webp) > 0
            AND (label IS NULL OR btrim(label) = ''))
    );
COMMENT ON COLUMN back_subject_question_choices.image_webp IS
    'WebP de la proposition quand elle n''a pas de texte. Null si la proposition est un texte.';

ALTER TABLE back_subject_question_pairs
    ALTER COLUMN question_side DROP NOT NULL,
    ALTER COLUMN answer_side DROP NOT NULL,
    ADD COLUMN question_image_webp BYTEA,
    ADD COLUMN answer_image_webp BYTEA;
ALTER TABLE back_subject_question_pairs DROP CONSTRAINT ck_back_subject_question_pairs_question;
ALTER TABLE back_subject_question_pairs DROP CONSTRAINT ck_back_subject_question_pairs_answer;
ALTER TABLE back_subject_question_pairs
    ADD CONSTRAINT ck_back_subject_question_pairs_question_face CHECK (
        (question_image_webp IS NULL AND question_side IS NOT NULL AND char_length(btrim(question_side)) > 0)
        OR (question_image_webp IS NOT NULL AND octet_length(question_image_webp) > 0
            AND (question_side IS NULL OR btrim(question_side) = ''))
    ),
    ADD CONSTRAINT ck_back_subject_question_pairs_answer_face CHECK (
        (answer_image_webp IS NULL AND answer_side IS NOT NULL AND char_length(btrim(answer_side)) > 0)
        OR (answer_image_webp IS NOT NULL AND octet_length(answer_image_webp) > 0
            AND (answer_side IS NULL OR btrim(answer_side) = ''))
    );

ALTER TABLE back_subject_question_items
    ALTER COLUMN label DROP NOT NULL,
    ADD COLUMN image_webp BYTEA;
ALTER TABLE back_subject_question_items DROP CONSTRAINT ck_back_subject_question_items_label;
ALTER TABLE back_subject_question_items
    ADD CONSTRAINT ck_back_subject_question_items_face CHECK (
        (image_webp IS NULL AND label IS NOT NULL AND char_length(btrim(label)) > 0)
        OR (image_webp IS NOT NULL AND octet_length(image_webp) > 0
            AND (label IS NULL OR btrim(label) = ''))
    );

ALTER TABLE back_subject_question_statements
    ALTER COLUMN label DROP NOT NULL,
    ADD COLUMN image_webp BYTEA;
ALTER TABLE back_subject_question_statements DROP CONSTRAINT ck_back_subject_question_statements_label;
ALTER TABLE back_subject_question_statements
    ADD CONSTRAINT ck_back_subject_question_statements_face CHECK (
        (image_webp IS NULL AND label IS NOT NULL AND char_length(btrim(label)) > 0)
        OR (image_webp IS NOT NULL AND octet_length(image_webp) > 0
            AND (label IS NULL OR btrim(label) = ''))
    );

ALTER TABLE back_subject_question_links
    ALTER COLUMN left_label DROP NOT NULL,
    ALTER COLUMN right_label DROP NOT NULL,
    ADD COLUMN left_image_webp BYTEA,
    ADD COLUMN right_image_webp BYTEA;
ALTER TABLE back_subject_question_links DROP CONSTRAINT ck_back_subject_question_links_left;
ALTER TABLE back_subject_question_links DROP CONSTRAINT ck_back_subject_question_links_right;
ALTER TABLE back_subject_question_links
    ADD CONSTRAINT ck_back_subject_question_links_left_face CHECK (
        (left_image_webp IS NULL AND left_label IS NOT NULL AND char_length(btrim(left_label)) > 0)
        OR (left_image_webp IS NOT NULL AND octet_length(left_image_webp) > 0
            AND (left_label IS NULL OR btrim(left_label) = ''))
    ),
    ADD CONSTRAINT ck_back_subject_question_links_right_face CHECK (
        (right_image_webp IS NULL AND right_label IS NOT NULL AND char_length(btrim(right_label)) > 0)
        OR (right_image_webp IS NOT NULL AND octet_length(right_image_webp) > 0
            AND (right_label IS NULL OR btrim(right_label) = ''))
    );

ALTER TABLE back_subject_question_cards
    ALTER COLUMN label DROP NOT NULL,
    ADD COLUMN image_webp BYTEA;
ALTER TABLE back_subject_question_cards DROP CONSTRAINT ck_back_subject_question_cards_label;
ALTER TABLE back_subject_question_cards
    ADD CONSTRAINT ck_back_subject_question_cards_face CHECK (
        (image_webp IS NULL AND label IS NOT NULL AND char_length(btrim(label)) > 0)
        OR (image_webp IS NOT NULL AND octet_length(image_webp) > 0
            AND (label IS NULL OR btrim(label) = ''))
    );

-- Le nom peut être vide avant de transformer une couleur déjà nommée.
ALTER TABLE back_subject_question_simon_elements
    ALTER COLUMN label DROP NOT NULL,
    ADD COLUMN image_webp BYTEA;
ALTER TABLE back_subject_question_simon_elements
    DROP CONSTRAINT ck_back_subject_question_simon_elements_label;

UPDATE back_subject_question_simon_elements
SET label = NULL
WHERE color IS NOT NULL;

ALTER TABLE back_subject_question_simon_elements
    ADD CONSTRAINT ck_back_subject_question_simon_elements_face CHECK (
        (image_webp IS NOT NULL AND octet_length(image_webp) > 0
            AND color IS NULL AND (label IS NULL OR btrim(label) = ''))
        OR (color IS NOT NULL AND image_webp IS NULL AND (label IS NULL OR btrim(label) = ''))
        OR (label IS NOT NULL AND char_length(btrim(label)) > 0
            AND color IS NULL AND image_webp IS NULL)
    );

ALTER TABLE back_subject_question_simon_options
    DROP CONSTRAINT ck_back_subject_question_simon_options_element_type;

COMMENT ON COLUMN back_subject_question_simon_elements.image_webp IS
    'WebP de l''élément quand il n''est ni un texte ni une couleur.';
COMMENT ON COLUMN back_subject_question_simon_elements.label IS
    'Texte libre de l''élément. Null si l''élément est une image ou une couleur.';
COMMENT ON COLUMN back_subject_question_simon_elements.color IS
    'Code #rrggbb quand l''élément est une couleur sans mot. Null sinon.';
