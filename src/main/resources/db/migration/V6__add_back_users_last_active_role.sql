-- SCRUM-124 : dernier espace choisi (parent ou teacher), survit à la déconnexion.

ALTER TABLE back_users
    ADD COLUMN last_active_role VARCHAR(32);

ALTER TABLE back_users
    ADD CONSTRAINT ck_back_users_last_active_role
        CHECK (last_active_role IS NULL OR last_active_role IN ('parent', 'teacher'));
