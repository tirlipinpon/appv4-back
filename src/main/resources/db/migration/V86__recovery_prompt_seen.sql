-- La personne a déjà vu la question sur le téléphone et le mail de contact.
-- Faux au départ : on peut encore la poser une fois.

ALTER TABLE back_users
    ADD COLUMN recovery_prompt_seen BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN back_users.recovery_prompt_seen IS
    'Vrai après que la personne a passé ou enregistré la sollicitation de récupération. Ne se remet pas à faux.';
