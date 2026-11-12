SELECT * FROM dev_front_demo.installation;
SELECT kind,count(*) AS tracked_roots FROM dev_front_demo.roots GROUP BY kind ORDER BY kind;
SELECT count(*) questions FROM back_subject_questions WHERE subject_id IN (SELECT id FROM dev_front_demo.roots WHERE kind='subject');
SELECT form_code,count(*) FROM back_subject_questions WHERE subject_id IN (SELECT id FROM dev_front_demo.roots WHERE kind='subject') GROUP BY form_code ORDER BY form_code;
