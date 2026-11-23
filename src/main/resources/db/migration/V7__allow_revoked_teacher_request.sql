-- SCRUM-142 : un admin peut reprendre un oui déjà donné

ALTER TABLE back_teacher_role_requests
    DROP CONSTRAINT ck_back_teacher_role_requests_status;

ALTER TABLE back_teacher_role_requests
    ADD CONSTRAINT ck_back_teacher_role_requests_status
    CHECK (status IN ('pending', 'approved', 'rejected', 'revoked'));
