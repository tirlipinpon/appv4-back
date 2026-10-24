-- SCRUM-159 : écoles et années où un professeur donne cours.
-- Le référentiel des écoles (table back_schools) arrive avec SCRUM-163.
-- school_id est un BIGINT pour pouvoir devenir une clé étrangère vers back_schools.id
-- sans changer de type. Aucune école n'est insérée ici.

CREATE TABLE back_teacher_schools (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    school_id   BIGINT NOT NULL,
    curriculum  VARCHAR(32) NOT NULL,
    CONSTRAINT uq_back_teacher_schools_user_school UNIQUE (user_id, school_id),
    CONSTRAINT ck_back_teacher_schools_curriculum CHECK (curriculum IN ('FRANCE', 'BELGIUM_FR'))
);

CREATE INDEX idx_back_teacher_schools_user_id ON back_teacher_schools (user_id);

CREATE TABLE back_teacher_school_grades (
    id                BIGSERIAL PRIMARY KEY,
    teacher_school_id BIGINT NOT NULL REFERENCES back_teacher_schools (id) ON DELETE CASCADE,
    grade_label       VARCHAR(80) NOT NULL,
    CONSTRAINT uq_back_teacher_school_grades_link_grade UNIQUE (teacher_school_id, grade_label)
);

CREATE INDEX idx_back_teacher_school_grades_link ON back_teacher_school_grades (teacher_school_id);

COMMENT ON TABLE back_teacher_schools IS
    'École où un professeur donne cours, avec la norme des années (France ou Belgique francophone). L''école elle-même n''est pas créée ici : school_id désignera back_schools (SCRUM-163).';

COMMENT ON COLUMN back_teacher_schools.school_id IS
    'Identifiant d''une école du référentiel. Pas de clé étrangère tant que back_schools n''existe pas (SCRUM-163).';

COMMENT ON COLUMN back_teacher_schools.curriculum IS
    'Norme des classes de cette école : FRANCE ou BELGIUM_FR. Deux écoles du même professeur peuvent avoir deux normes.';

COMMENT ON TABLE back_teacher_school_grades IS
    'Années scolaires cochées pour une école du professeur (CP, 1re primaire, etc.). Pas l''année civile.';
