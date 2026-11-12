-- IDs, never email/name prefixes, determine ownership of the fixture.
DO $$ DECLARE t record; BEGIN
 FOR t IN SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename LIKE 'back_%' ORDER BY tablename LOOP
  EXECUTE format('LOCK TABLE public.%I IN SHARE ROW EXCLUSIVE MODE',t.tablename);
 END LOOP;
END $$;
CREATE TEMP TABLE demo_users ON COMMIT DROP AS SELECT id FROM dev_front_demo.roots WHERE kind='user';
CREATE TEMP TABLE demo_schools ON COMMIT DROP AS SELECT id FROM dev_front_demo.roots WHERE kind='school';
CREATE TEMP TABLE demo_children ON COMMIT DROP AS
SELECT id FROM dev_front_demo.roots WHERE kind='child'
UNION SELECT id FROM back_children WHERE parent_user_id IN (SELECT id FROM demo_users);
CREATE TEMP TABLE demo_subjects ON COMMIT DROP AS
SELECT id FROM dev_front_demo.roots WHERE kind='subject'
UNION SELECT id FROM back_teacher_subjects WHERE user_id IN (SELECT id FROM demo_users);
-- Stop if real and demo records were mixed through the UI after installation.
-- Lock business tables to prevent a concurrent request from creating a cross-link
-- between the checks and the deletes. Readers remain allowed.
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM back_children WHERE
   (id IN (SELECT id FROM demo_children) AND parent_user_id NOT IN (SELECT id FROM demo_users)) OR
   (school_id IN (SELECT id FROM demo_schools) AND parent_user_id NOT IN (SELECT id FROM demo_users)))
 OR EXISTS(SELECT 1 FROM back_teacher_subjects WHERE
   (id IN (SELECT id FROM demo_subjects) AND user_id NOT IN (SELECT id FROM demo_users)) OR
   (school_id IN (SELECT id FROM demo_schools) AND user_id NOT IN (SELECT id FROM demo_users)) OR
   (origin_subject_id IN (SELECT id FROM demo_subjects) AND id NOT IN (SELECT id FROM demo_subjects)))
 OR EXISTS(SELECT 1 FROM back_teacher_schools WHERE school_id IN (SELECT id FROM demo_schools) AND user_id NOT IN (SELECT id FROM demo_users))
 OR EXISTS(SELECT 1 FROM back_teacher_subject_access WHERE
   (subject_id IN (SELECT id FROM demo_subjects)) <> (user_id IN (SELECT id FROM demo_users)))
 OR EXISTS(SELECT 1 FROM back_child_subject_links WHERE
   (subject_id IN (SELECT id FROM demo_subjects)) <> (child_id IN (SELECT id FROM demo_children)))
 OR EXISTS(SELECT 1 FROM back_child_links WHERE
   (subject_child_id IN (SELECT id FROM demo_children)) <> (related_child_id IN (SELECT id FROM demo_children)) OR
   (subject_child_id IN (SELECT id FROM demo_children)) <> (parent_user_id IN (SELECT id FROM demo_users)))
 OR EXISTS(SELECT 1 FROM back_teacher_subject_transfer_requests WHERE
   (subject_id IN (SELECT id FROM demo_subjects)) <> (from_user_id IN (SELECT id FROM demo_users)) OR
   (subject_id IN (SELECT id FROM demo_subjects)) <> (to_user_id IN (SELECT id FROM demo_users)))
 OR EXISTS(SELECT 1 FROM back_teacher_subject_deactivate_notices WHERE
   (subject_id IN (SELECT id FROM demo_subjects)) <> (from_user_id IN (SELECT id FROM demo_users)) OR
   (subject_id IN (SELECT id FROM demo_subjects)) <> (to_user_id IN (SELECT id FROM demo_users))) THEN
   RAISE EXCEPTION 'Real/demo cross-links detected. Unlink them in the app before rollback; nothing deleted.';
 END IF;
END $$;
DELETE FROM back_teacher_subjects WHERE id IN (SELECT id FROM demo_subjects);
DELETE FROM back_children WHERE id IN (SELECT id FROM demo_children);
DELETE FROM back_users WHERE id IN (SELECT id FROM demo_users);
DELETE FROM back_schools WHERE id IN (SELECT id FROM demo_schools);
DELETE FROM dev_front_demo.roots;
DELETE FROM dev_front_demo.installation;
