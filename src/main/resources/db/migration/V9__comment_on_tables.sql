-- Descriptif court de chaque table métier, visible dans les outils SQL (pg_description).

COMMENT ON TABLE front_hello_messages IS
    'Messages de test (smoke) de l''app learning. Servis par GET /api/ext/hello pour vérifier que le front parle à la base.';

COMMENT ON TABLE back_hello_messages IS
    'Messages de test (smoke) de l''admin. Servis par GET /api/int/hello pour vérifier que le back parle à la base.';

COMMENT ON TABLE back_users IS
    'Comptes (parent, professeur, admin) : email, mot de passe hashé, nom affiché, compte actif, et dernier espace choisi (parent ou teacher).';

COMMENT ON TABLE back_user_identities IS
    'Lien entre un compte et un réseau social (Google, Facebook ou Apple). Permet de se connecter sans mot de passe local.';

COMMENT ON TABLE back_password_reset_tokens IS
    'Jetons à usage unique pour retrouver un mot de passe oublié. Seul le hash du jeton est stocké, avec une date d''expiration.';

COMMENT ON TABLE back_user_roles IS
    'Rôles déjà accordés à un compte : parent dès l''inscription, teacher après validation admin, ou admin.';

COMMENT ON TABLE back_teacher_role_requests IS
    'Demandes pour devenir professeur. Statuts : en attente, acceptée, refusée, ou retirée par un admin. Tant que c''est en attente, le rôle teacher n''est pas accordé.';

COMMENT ON TABLE flyway_schema_history IS
    'Historique des migrations Flyway déjà jouées. Table technique de l''outil, pas une donnée métier.';
