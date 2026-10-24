-- Index redondants : la contrainte UNIQUE couvre déjà les recherches d'égalité.
-- Ne pas retoucher V3 / V5 (déjà appliquées).

-- back_users.email : uq_back_users_email crée déjà un B-tree sur email.
DROP INDEX IF EXISTS idx_back_users_email;

-- back_user_roles.user_id : préfixe de uq_back_user_roles_user_role (user_id, role).
-- findByUserId peut s'appuyer sur cet index unique composite.
DROP INDEX IF EXISTS idx_back_user_roles_user_id;

-- Conservé volontairement : idx_back_teacher_role_requests_user_id.
-- L'index unique partiel ne couvre que status = 'pending', pas toutes les lectures par user_id.
