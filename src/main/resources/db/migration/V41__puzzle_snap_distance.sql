-- SCRUM-183 : distance d'aimantation d'une pièce vers sa découpe.
-- Fraction de la largeur de l'image. 0,08 = 8 %. Les puzzles déjà enregistrés prennent cette valeur.

ALTER TABLE back_subject_question_puzzle_images
    ADD COLUMN snap_distance DOUBLE PRECISION NOT NULL DEFAULT 0.08;

ALTER TABLE back_subject_question_puzzle_images
    ADD CONSTRAINT ck_back_subject_question_puzzle_images_snap
        CHECK (snap_distance >= 0 AND snap_distance <= 0.25);

COMMENT ON COLUMN back_subject_question_puzzle_images.snap_distance IS
    'Magnétisme : fraction de la largeur de l''image. Une pièce lâche dans ce rayon se colle sur sa découpe.';
