-- Instant du dernier niveau, pour trier les gains du même jour.
ALTER TABLE back_child_badges
    ADD COLUMN gained_at TIMESTAMPTZ;

COMMENT ON COLUMN back_child_badges.gained_at IS
    'Instant du dernier franchissement de niveau. Les gains du jour se lisent du plus récent au plus ancien. Null si ce franchissement n''a pas été daté.';
