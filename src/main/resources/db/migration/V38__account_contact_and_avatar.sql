-- Mail de contact, téléphone et avatar du compte. Le mail de connexion reste la colonne email.
ALTER TABLE back_users
    ADD COLUMN contact_email VARCHAR(320),
    ADD COLUMN phone VARCHAR(32),
    ADD COLUMN avatar_code VARCHAR(32);
