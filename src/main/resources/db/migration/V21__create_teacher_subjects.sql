-- SCRUM-161 : matières d'un professeur, pour une école et une année déjà cochées.
-- Chaque professeur ne voit que les siennes. Le nom affiché est conservé tel quel,
-- la comparaison ignore la casse (name_key).
--
-- Plus tard, sans changer le rôle de cette table :
--   * cloner : une nouvelle ligne, origin_subject_id = id d'origine, autre user_id.
--     Retirer l'originale ne retire pas la copie (pas de clé étrangère sur origin_subject_id).
--   * échanger : changer user_id. Les formes de questions suivent la matière.
-- Les exercices ne sont pas ici.

CREATE TABLE back_teacher_subjects (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT NOT NULL REFERENCES back_users (id) ON DELETE CASCADE,
    school_id          BIGINT NOT NULL,
    grade_label        VARCHAR(80) NOT NULL,
    name               VARCHAR(100) NOT NULL,
    name_key           VARCHAR(100) NOT NULL,
    origin_subject_id  BIGINT,
    CONSTRAINT uq_back_teacher_subjects_owner_class_name UNIQUE (user_id, school_id, grade_label, name_key),
    CONSTRAINT ck_back_teacher_subjects_name_not_blank CHECK (char_length(btrim(name)) > 0)
);

CREATE INDEX idx_back_teacher_subjects_user_id ON back_teacher_subjects (user_id);

COMMENT ON TABLE back_teacher_subjects IS
    'Matière créée par un professeur pour une école et une année de son profil. Pas un exercice.';

COMMENT ON COLUMN back_teacher_subjects.school_id IS
    'École du référentiel (back_schools). Pas de clé étrangère : le profil professeur range déjà school_id ainsi (SCRUM-159).';

COMMENT ON COLUMN back_teacher_subjects.grade_label IS
    'Année scolaire cochée sur le profil (CE1, 3e primaire, …). Pas l''année civile.';

COMMENT ON COLUMN back_teacher_subjects.name IS
    'Nom affiché, sans espaces autour, 100 caractères maximum.';

COMMENT ON COLUMN back_teacher_subjects.name_key IS
    'Nom en minuscules, pour refuser Maths et maths dans la même classe.';

COMMENT ON COLUMN back_teacher_subjects.origin_subject_id IS
    'Vide quand le professeur crée la matière. Un clone futur recopiera la ligne et remplira cette colonne. Pas de clé étrangère.';

-- Formes de questions d'une matière (exemple : plusieurs formes pour Mathématiques).
-- Cette story n'insère aucune ligne et n'ouvre aucune route. Le clone futur copie ces lignes avec la matière.
CREATE TABLE back_subject_question_forms (
    id          BIGSERIAL PRIMARY KEY,
    subject_id  BIGINT NOT NULL REFERENCES back_teacher_subjects (id) ON DELETE CASCADE,
    form_code   VARCHAR(40) NOT NULL,
    label       VARCHAR(100) NOT NULL,
    sort_order  INTEGER NOT NULL,
    CONSTRAINT uq_back_subject_question_forms_subject_code UNIQUE (subject_id, form_code)
);

CREATE INDEX idx_back_subject_question_forms_subject ON back_subject_question_forms (subject_id);

COMMENT ON TABLE back_subject_question_forms IS
    'Forme de question prévue pour une matière. Aucune ligne dans SCRUM-161 : les exercices viendront ensuite.';
