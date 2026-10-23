-- SCRUM-142 : la date du oui reste, la date du retrait est à part

ALTER TABLE back_teacher_role_requests
    ADD COLUMN revoked_at TIMESTAMPTZ;

UPDATE back_teacher_role_requests
SET revoked_at = reviewed_at
WHERE status = 'revoked'
  AND revoked_at IS NULL;
