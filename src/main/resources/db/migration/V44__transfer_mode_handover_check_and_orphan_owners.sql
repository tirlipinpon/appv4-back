-- Handover : la contrainte V29 n'autorisait que move/share/ask → INSERT mode=handover en 500.
-- Orphelins : propriétaire (user_id) sans ligne d'accès → réassigner au premier détenteur restant.

ALTER TABLE back_teacher_subject_transfer_requests
    DROP CONSTRAINT IF EXISTS ck_transfer_request_mode;

ALTER TABLE back_teacher_subject_transfer_requests
    ADD CONSTRAINT ck_transfer_request_mode
        CHECK (mode IN ('move', 'share', 'ask', 'handover'));

COMMENT ON COLUMN back_teacher_subject_transfer_requests.mode IS
    'move, share, ask ou handover (reprise de propriété par un co-détenteur).';

UPDATE back_teacher_subjects s
SET user_id = sub.holder_id
FROM (
    SELECT DISTINCT ON (a.subject_id)
           a.subject_id,
           a.user_id AS holder_id
    FROM back_teacher_subject_access a
    INNER JOIN back_teacher_subjects orphan
            ON orphan.id = a.subject_id
           AND orphan.hidden = false
    WHERE NOT EXISTS (
        SELECT 1
        FROM back_teacher_subject_access own
        WHERE own.subject_id = orphan.id
          AND own.user_id = orphan.user_id
    )
    ORDER BY a.subject_id, a.id ASC
) sub
WHERE s.id = sub.subject_id;
