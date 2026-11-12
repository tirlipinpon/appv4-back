DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM dev_front_demo.installation) THEN
   RAISE EXCEPTION 'Demo already installed. Use status, rollback or reset (no duplicates created).';
 END IF;
 IF (SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1) <> '47' THEN
   RAISE EXCEPTION 'Fixtures validated against V47. Review schema changes before updating this guard.';
 END IF;
END $$;

CREATE TEMP TABLE schools ON COMMIT DROP AS
SELECT n, nextval('back_schools_id_seq') id,
 CASE WHEN n%2=0 THEN 'FRANCE' ELSE 'BELGIUM_FR' END curriculum
FROM generate_series(1,12) n;
INSERT INTO back_schools(id,name,country,teaching_types,network,street,postal_code,city,province)
SELECT id, '[DEMO] ' || (ARRAY['École des Tilleuls','Institut Saint-Exupéry','Campus international des Arts et des Sciences – site du Parc'])[1+n%3] || ' ' || n,
 CASE WHEN curriculum='FRANCE' THEN 'France' ELSE 'Belgique' END,
 'Maternelle, Primaire, Secondaire, Collège, Lycée',
 (ARRAY['Officiel subventionné','Libre non confessionnel','Communal'])[1+n%3],
 'Rue des Démonstrations '||n, CASE WHEN n%2=0 THEN '59000' ELSE '1000' END,
 (ARRAY['Bruxelles','Lille','Namur','Lyon'])[1+n%4], NULL
FROM schools;
INSERT INTO dev_front_demo.roots SELECT 'school',id FROM schools;

CREATE TEMP TABLE grades ON COMMIT DROP AS
SELECT s.*, g.grade, g.ord::int grade_n FROM schools s
CROSS JOIN LATERAL unnest(CASE WHEN s.curriculum='FRANCE' THEN
 ARRAY['Petite section (PS)','Moyenne section (MS)','Grande section (GS)','CP','CE1','CE2','CM1','CM2','6e','5e','4e','3e','2de','1re','Terminale']
 ELSE ARRAY['1re maternelle','2e maternelle','3e maternelle','1re primaire','2e primaire','3e primaire','4e primaire','5e primaire','6e primaire','1re secondaire','2e secondaire','3e secondaire','4e secondaire','5e secondaire','6e secondaire','7e année (enseignement professionnel)'] END)
 WITH ORDINALITY g(grade,ord);

CREATE TEMP TABLE users ON COMMIT DROP AS
SELECT n, nextval('back_users_id_seq') id, CASE WHEN n<=360 THEN 'teacher' ELSE 'parent' END role
FROM generate_series(1,600) n;
INSERT INTO back_users(id,email,password_hash,display_name,last_active_role,initial_roles_completed,avatar_code,created_at)
SELECT id, role||lpad((CASE WHEN n<=360 THEN n ELSE n-360 END)::text,3,'0')||'@front-demo.example.test',
 '$2b$10$gzBCM7T/fm4wt/6uG.3W8uuGndRwsk4OugWXSW4Phkq74gfMsNxKu',
 (ARRAY['Élodie','Jean-Baptiste','Anaïs','Noé','Zoé','Mohamed','Léa','François'])[1+n%8] || ' ' ||
 (ARRAY['D’Haene','Van den Broeck','Martin','De la Fontaine','Dupont','Lefèvre'])[1+n%6] || ' · Démo '||n,
 role,true,(ARRAY['owl','fox','bear','cat','panda','frog'])[1+n%6],now()-(n||' days')::interval
FROM users;
INSERT INTO dev_front_demo.roots SELECT 'user',id FROM users;
INSERT INTO back_user_roles(user_id,role) SELECT id,role FROM users;
INSERT INTO back_user_roles(user_id,role) SELECT id,'parent' FROM users WHERE n<=360 AND n%12=0;
INSERT INTO back_teacher_role_requests(user_id,status,reviewed_at)
SELECT id,'approved',now() FROM users WHERE role='teacher';
INSERT INTO back_teacher_role_requests(user_id,status,reviewed_at)
SELECT id,CASE n%3 WHEN 0 THEN 'pending' WHEN 1 THEN 'rejected' ELSE 'revoked' END,
 CASE WHEN n%3=0 THEN NULL ELSE now() END FROM users WHERE role='parent' AND n%7=0;

CREATE TEMP TABLE assignments ON COMMIT DROP AS
SELECT nextval('back_teacher_schools_id_seq') id,u.id user_id,u.n,s.id school_id,s.curriculum
FROM users u JOIN schools s ON s.n=1+(u.n-1)%12 OR (u.n%4=0 AND s.n=1+u.n%12)
WHERE u.role='teacher';
INSERT INTO back_teacher_schools(id,user_id,school_id,curriculum)
SELECT id,user_id,school_id,curriculum FROM assignments;
INSERT INTO back_teacher_school_grades(teacher_school_id,grade_label)
SELECT a.id,g.grade FROM assignments a JOIN grades g ON g.id=a.school_id
WHERE (g.grade_n+(a.n-1)/12)%5 < 2+a.n%4;

CREATE TEMP TABLE subjects ON COMMIT DROP AS
SELECT nextval('back_teacher_subjects_id_seq') id,g.id school_id,g.grade,g.grade_n,g.n school_n,k,
 (SELECT a.user_id FROM assignments a JOIN back_teacher_school_grades ag ON ag.teacher_school_id=a.id
  WHERE a.school_id=g.id AND ag.grade_label=g.grade ORDER BY (a.n+k)%360,a.n LIMIT 1) owner,
 (ARRAY['Mathématiques','Français – lecture et expression','Sciences et découverte du monde','Histoire & géographie','Langues : néerlandais et anglais'])[k] name
FROM grades g CROSS JOIN LATERAL generate_series(1,2+(g.n+g.grade_n)%4) k;
INSERT INTO back_teacher_subjects(id,user_id,school_id,grade_label,name,name_key,active,hidden)
SELECT id,owner,school_id,grade,name,lower(name),true,false FROM subjects;
INSERT INTO dev_front_demo.roots SELECT 'subject',id FROM subjects;
INSERT INTO back_teacher_subject_access(subject_id,user_id,active)
SELECT id,owner,true FROM subjects;
INSERT INTO back_teacher_subject_access(subject_id,user_id,active)
SELECT s.id,a.user_id,(a.n+s.grade_n)%17<>0 FROM subjects s
JOIN assignments a ON a.school_id=s.school_id
JOIN back_teacher_school_grades g ON g.teacher_school_id=a.id AND g.grade_label=s.grade
WHERE a.user_id<>s.owner AND (a.n+s.k)%3=0;

-- Children: every parent has 1..5; dual-role teachers also have families.
CREATE TEMP TABLE kids ON COMMIT DROP AS
SELECT nextval('back_children_id_seq') id,u.id parent,u.n,k,
 1+(u.n+CASE WHEN u.n%3=0 THEN 0 ELSE k-1 END-1)%12 school_n FROM users u CROSS JOIN LATERAL generate_series(1,1+(u.n-1)%5) k
WHERE u.role='parent' OR u.n%12=0;
INSERT INTO back_children(id,parent_user_id,first_name,last_name,age,curriculum,grade_label,school_id,avatar_code,pin_hash,sex,active,hidden)
SELECT c.id,c.parent,(ARRAY['Emma','Louis','Maël','Inès','Chloé','Ali','Lou','Émile'])[1+(c.n+c.k)%8],
 'Famille Démo '||c.n,least(18,g.grade_n+2),g.curriculum,g.grade,
 CASE WHEN c.n%23=0 AND c.k=1 THEN NULL ELSE g.id END,
 (ARRAY['owl','fox','bear','cat','panda','frog'])[1+(c.n+c.k)%6],
 '$2b$10$Zw30SLv1CwF8kGr6sHU0ye4wq8970SXZFAtPaJeesS5UKoaNbzUmm',
 (ARRAY['GARCON','FILLE','NON_GENRE'])[1+(c.n+c.k)%3],
 NOT(c.n%19=0 AND c.k=5),(c.n%29=0 AND c.k=4)
FROM kids c JOIN grades g ON g.n=c.school_n AND g.grade_n=1+(c.n+CASE WHEN c.n%10=0 AND c.k<=2 THEN 1 ELSE c.k END)%15;
INSERT INTO dev_front_demo.roots SELECT 'child',id FROM kids;
INSERT INTO back_child_links(parent_user_id,subject_child_id,related_child_id,relation)
SELECT c.parent,c.id,b.id,CASE WHEN c.n%10=0 AND c.k=2 THEN CASE WHEN ch.sex='FILLE' THEN 'JUMELLE' ELSE 'JUMEAU' END WHEN ch.sex='FILLE' THEN
 (ARRAY['SOEUR','DEMI_SOEUR','COUSINE'])[1+c.n%3] ELSE
 (ARRAY['FRERE','DEMI_FRERE','COUSIN'])[1+c.n%3] END
FROM kids c JOIN kids b ON b.parent=c.parent AND b.k=1 JOIN back_children ch ON ch.id=c.id WHERE c.k>1;
INSERT INTO back_child_subject_links(child_id,subject_id,excluded)
SELECT c.id,s.id,(k.n%11=0 AND s.k=2) FROM kids k JOIN back_children c ON c.id=k.id
JOIN subjects s ON s.school_id=c.school_id AND s.grade=c.grade_label;
INSERT INTO back_child_subject_links(child_id,subject_id,excluded)
SELECT c.id,s.id,false FROM kids c JOIN subjects s ON s.school_n=1+c.school_n%12
AND s.grade_n=1+c.n%15 AND s.k=1 WHERE c.n%3=0 ON CONFLICT DO NOTHING;

-- Pending invitations: shared holder for handover; no access yet for share/move/ask.
INSERT INTO back_teacher_subject_transfer_requests(subject_id,from_user_id,to_user_id,mode,status)
SELECT s.id,CASE WHEN s.k=3 THEN candidate.user_id ELSE s.owner END,
 CASE WHEN s.k=3 THEN s.owner ELSE candidate.user_id END,
 (ARRAY['share','move','ask','handover'])[1+(s.k-1)%4], 'pending'
FROM subjects s CROSS JOIN LATERAL (
 SELECT a.user_id FROM assignments a JOIN back_teacher_school_grades g ON g.teacher_school_id=a.id
 WHERE a.school_id=s.school_id AND g.grade_label=s.grade AND a.user_id<>s.owner
 AND (EXISTS(SELECT 1 FROM back_teacher_subject_access x WHERE x.subject_id=s.id AND x.user_id=a.user_id))=(s.k=4)
 ORDER BY a.n LIMIT 1
) candidate WHERE s.grade_n%3=0 AND s.k<=4;

CREATE TEMP TABLE forms(code,label,ord) ON COMMIT DROP AS VALUES
 ('qcm','Choix multiples',1),('memory','Memory',2),('order','Ordre',3),
 ('vrai_faux','Vrai ou faux',4),('liens','Liens',5),('case_vide','Case vide',6),
 ('texte','Texte',7),('image_interactive','Image à cliquer',8),('reponse_libre','Lettre par lettre',9),('puzzle','Puzzle',10);
INSERT INTO back_subject_question_forms(subject_id,form_code,label,sort_order)
SELECT s.id,f.code,CASE WHEN f.code='texte' THEN 'Réponse libre' ELSE f.label END,
 CASE WHEN f.code='texte' THEN 11 ELSE f.ord END FROM subjects s CROSS JOIN forms f;
CREATE TEMP TABLE questions ON COMMIT DROP AS
SELECT nextval('back_subject_questions_id_seq') id,s.id subject_id,f.code,n,s.k
FROM subjects s CROSS JOIN LATERAL generate_series(1,200+50*((s.school_n+s.grade_n+s.k)%5)) n
JOIN forms f ON f.ord=1+(n-1)%10;
INSERT INTO back_subject_questions(id,subject_id,form_code,prompt)
SELECT q.id,q.subject_id,q.code,
 CASE WHEN q.n%13=0 AND q.code IN ('memory','order','vrai_faux','liens','case_vide') THEN '' ELSE
 s.name||' · '||s.grade||' · Activité '||q.n||' : '||
 CASE q.code WHEN 'qcm' THEN 'Sélectionne les propositions correctes.' WHEN 'order' THEN 'Replace les étapes dans l’ordre.'
 WHEN 'texte' THEN 'Écris le mot école.' WHEN 'reponse_libre' THEN 'Compose le mot école.'
 WHEN 'case_vide' THEN 'Complète la phrase.' WHEN 'image_interactive' THEN 'Clique sur la zone bleue.'
 WHEN 'puzzle' THEN 'Recompose les deux moitiés de l’image.' ELSE 'Observe puis associe les éléments.' END ||
 CASE WHEN q.n%7=0 THEN ' Lis attentivement chaque proposition, prends le temps de comparer les réponses et vérifie ton choix avant de continuer.' ELSE '' END END
FROM questions q JOIN subjects s ON s.id=q.subject_id;
INSERT INTO back_subject_question_choices(question_id,label,correct,sort_order)
SELECT q.id,'Proposition '||i||' — activité '||q.n, i=0 OR (q.n%3=0 AND i=2),i
FROM questions q CROSS JOIN LATERAL generate_series(0,1+(q.n/10)%5) i WHERE code='qcm';
INSERT INTO back_subject_question_pairs(question_id,question_side,answer_side,sort_order)
SELECT q.id,'Mot '||i||' · '||q.n,'Définition '||i||' · '||q.n,i FROM questions q
CROSS JOIN LATERAL generate_series(0,1+(q.n/10)%5) i WHERE code='memory';
INSERT INTO back_subject_question_items(question_id,label,sort_order)
SELECT q.id,'Étape '||(i+1)||' de l’activité '||q.n,i FROM questions q
CROSS JOIN LATERAL generate_series(0,1+(q.n/10)%6) i WHERE code='order';
INSERT INTO back_subject_question_statements(question_id,label,truthful,sort_order)
SELECT q.id,i||' + 1 = '||(i+CASE WHEN i%2=0 THEN 1 ELSE 2 END),i%2=0,i FROM questions q
CROSS JOIN LATERAL generate_series(0,1+(q.n/10)%4) i WHERE code='vrai_faux';
INSERT INTO back_subject_question_links(question_id,left_label,right_label,sort_order)
SELECT q.id,'Notion '||i||' · '||q.n,'Exemple '||i||' · '||q.n,i FROM questions q
CROSS JOIN LATERAL generate_series(0,1+(q.n/10)%5) i WHERE code='liens';
INSERT INTO back_subject_question_gaps(question_id,sentence,word_label,decoy,sort_order)
SELECT q.id,'Le ___ lit un ___ à l’école.',v.word,v.decoy,v.ord FROM questions q
CROSS JOIN (VALUES ('garçon',false,0),('livre',false,1),('nuage',true,0),('rouge',true,1)) v(word,decoy,ord) WHERE code='case_vide';
INSERT INTO back_subject_question_texts SELECT id,'école' FROM questions WHERE code='texte';
INSERT INTO back_subject_question_letters SELECT id,'école' FROM questions WHERE code='reponse_libre';
-- A small, valid WebP illustration is stored once in the script and reused.
INSERT INTO back_subject_question_click_images(question_id,image_webp,require_all_correct)
SELECT id,decode(:'demo_image','hex'),n%3=0 FROM questions WHERE code='image_interactive';
INSERT INTO back_subject_question_click_zones(question_id,label,correct,shape,points,sort_order)
SELECT q.id,v.label,v.correct,v.shape,v.points,v.ord FROM questions q CROSS JOIN
 (VALUES ('Bleu',true,'rect','0,0;0.5,1',0),('Orange',false,'rect','0.5,0;1,1',1)) v(label,correct,shape,points,ord)
WHERE code='image_interactive';
INSERT INTO back_subject_question_puzzle_images(question_id,image_webp,snap_distance)
SELECT id,decode(:'demo_image','hex'),0.04+0.02*(n%5) FROM questions WHERE code='puzzle';
INSERT INTO back_subject_question_puzzle_pieces(question_id,label,shape,points,sort_order)
SELECT q.id,v.label,'rect',v.points,v.ord FROM questions q CROSS JOIN
 (VALUES ('Moitié gauche','0,0;0.5,1',0),('Moitié droite','0.5,0;1,1',1)) v(label,points,ord) WHERE code='puzzle';

-- Separate archived copies keep 2..5 visible, active subjects in every class.
WITH added AS (
 INSERT INTO back_teacher_subjects(user_id,school_id,grade_label,name,name_key,active,hidden,origin_subject_id)
 SELECT owner,school_id,grade,'Archives – '||name,lower('Archives – '||name),false,grade_n%8=0,id
 FROM subjects WHERE k=1 AND grade_n%4=0 RETURNING id
) INSERT INTO dev_front_demo.roots SELECT 'subject',id FROM added;
INSERT INTO back_teacher_subject_access(subject_id,user_id,active)
SELECT s.id,s.user_id,false FROM back_teacher_subjects s JOIN dev_front_demo.roots r ON r.kind='subject' AND r.id=s.id
WHERE NOT s.active AND NOT s.hidden;
INSERT INTO back_teacher_subject_transfer_requests(subject_id,from_user_id,to_user_id,mode,status,created_at)
SELECT subject_id,from_user_id,to_user_id,mode,
 CASE WHEN subject_id%2=0 THEN 'rejected' ELSE 'cancelled' END,now()-interval '15 days'
FROM back_teacher_subject_transfer_requests WHERE subject_id IN (SELECT id FROM subjects);
INSERT INTO back_teacher_subject_transfer_requests(subject_id,from_user_id,to_user_id,mode,status,created_at)
SELECT s.id,s.owner,a.user_id,'share','accepted',now()-interval '30 days'
FROM subjects s JOIN back_teacher_subject_access a ON a.subject_id=s.id AND a.user_id<>s.owner
WHERE s.grade_n%5=0 AND a.active;
INSERT INTO back_teacher_subject_deactivate_notices(subject_id,from_user_id,to_user_id,deactivated_user_ids,message,dismissed_at)
SELECT s.id,s.owner,a.user_id,a.user_id::text,'Le propriétaire a désactivé votre accès à cette matière de démonstration.',
 CASE WHEN s.grade_n%2=0 THEN now() ELSE NULL END
FROM subjects s JOIN back_teacher_subject_access a ON a.subject_id=s.id WHERE NOT a.active;
INSERT INTO dev_front_demo.installation(version) VALUES(1);
