-- Notices matières génériques (désactivation, handover) + mode handover sur les demandes.

ALTER TABLE back_teacher_subject_deactivate_notices
    ADD COLUMN kind VARCHAR(40) NOT NULL DEFAULT 'deactivate';

ALTER TABLE back_teacher_subject_deactivate_notices
    ADD COLUMN message VARCHAR(500);

COMMENT ON COLUMN back_teacher_subject_deactivate_notices.kind IS
    'Type d''alerte : deactivate, handover_accepted, handover_rejected, …';

COMMENT ON COLUMN back_teacher_subject_deactivate_notices.message IS
    'Texte prêt à afficher. Si null (anciennes lignes), le back le recalcule.';

ALTER TABLE back_teacher_subject_transfer_requests
    ALTER COLUMN mode TYPE VARCHAR(20);

COMMENT ON COLUMN back_teacher_subject_transfer_requests.mode IS
    'move, share, ask ou handover (reprise de propriété par un co-détenteur).';
