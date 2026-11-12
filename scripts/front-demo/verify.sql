DO $$ BEGIN
 IF NOT EXISTS (SELECT 1 FROM dev_front_demo.installation) THEN RAISE EXCEPTION 'Demo not installed'; END IF;
 IF EXISTS (
  SELECT 1 FROM back_users u JOIN dev_front_demo.roots r ON r.kind='user' AND r.id=u.id
  JOIN back_user_roles role ON role.user_id=u.id AND role.role='parent'
  LEFT JOIN back_children c ON c.parent_user_id=u.id GROUP BY u.id HAVING count(c.id) NOT BETWEEN 1 AND 5
 ) THEN RAISE EXCEPTION 'Invalid family size'; END IF;
 IF EXISTS (
  SELECT 1 FROM back_teacher_subjects s JOIN dev_front_demo.roots r ON r.kind='subject' AND r.id=s.id
  WHERE NOT s.hidden AND s.active AND NOT EXISTS (
   SELECT 1 FROM back_teacher_subject_access a JOIN back_teacher_schools t ON t.user_id=a.user_id AND t.school_id=s.school_id
   JOIN back_teacher_school_grades g ON g.teacher_school_id=t.id AND g.grade_label=s.grade_label
   WHERE a.subject_id=s.id AND a.user_id=s.user_id AND a.active
  )
 ) THEN RAISE EXCEPTION 'Owner without valid school/grade/access'; END IF;
 IF EXISTS (
  SELECT 1 FROM back_teacher_subjects s JOIN dev_front_demo.roots r ON r.kind='subject' AND r.id=s.id
  WHERE s.active AND NOT s.hidden GROUP BY school_id,grade_label HAVING count(*) NOT BETWEEN 2 AND 5
 ) THEN RAISE EXCEPTION 'Invalid subjects per class'; END IF;
 IF EXISTS (
  SELECT 1 FROM back_teacher_subjects s JOIN dev_front_demo.roots r ON r.kind='subject' AND r.id=s.id
  LEFT JOIN back_subject_questions q ON q.subject_id=s.id WHERE s.active AND NOT s.hidden
  GROUP BY s.id HAVING count(q.id) NOT BETWEEN 200 AND 400 OR count(DISTINCT q.form_code)<>10
 ) THEN RAISE EXCEPTION 'Invalid question counts/forms'; END IF;
 IF EXISTS (
  SELECT 1 FROM back_subject_questions q JOIN dev_front_demo.roots r ON r.kind='subject' AND r.id=q.subject_id
  WHERE CASE q.form_code
   WHEN 'qcm' THEN NOT EXISTS(SELECT 1 FROM back_subject_question_choices c WHERE c.question_id=q.id AND correct)
   WHEN 'memory' THEN (SELECT count(*) FROM back_subject_question_pairs c WHERE c.question_id=q.id)<2
   WHEN 'order' THEN (SELECT count(*) FROM back_subject_question_items c WHERE c.question_id=q.id)<2
   WHEN 'vrai_faux' THEN NOT EXISTS(SELECT 1 FROM back_subject_question_statements c WHERE c.question_id=q.id)
   WHEN 'liens' THEN (SELECT count(*) FROM back_subject_question_links c WHERE c.question_id=q.id)<2
   WHEN 'case_vide' THEN (SELECT count(*) FROM back_subject_question_gaps c WHERE c.question_id=q.id AND NOT decoy)<>2
   WHEN 'texte' THEN NOT EXISTS(SELECT 1 FROM back_subject_question_texts c WHERE c.question_id=q.id)
   WHEN 'reponse_libre' THEN NOT EXISTS(SELECT 1 FROM back_subject_question_letters c WHERE c.question_id=q.id)
   WHEN 'image_interactive' THEN NOT EXISTS(SELECT 1 FROM back_subject_question_click_images c WHERE c.question_id=q.id)
   WHEN 'puzzle' THEN NOT EXISTS(SELECT 1 FROM back_subject_question_puzzle_images c WHERE c.question_id=q.id)
   ELSE true END
 ) THEN RAISE EXCEPTION 'Missing question content'; END IF;
END $$;
SELECT 'Demo integrity OK' AS result;
