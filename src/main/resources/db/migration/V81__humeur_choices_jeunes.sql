-- Huit humeurs de départ, mots courts pour les enfants (ordre de l'image).

INSERT INTO back_humeur_choices (icon, word)
SELECT seed.icon, seed.word
FROM (
    VALUES
        (1, '😊', 'Joyeux'),
        (2, '😠', 'Fâché'),
        (3, '🥰', 'Aimé'),
        (4, '😣', 'Embêté'),
        (5, '😜', 'Rigolo'),
        (6, '😮', 'Surpris'),
        (7, '😒', 'Agacé'),
        (8, '😄', 'Drôle')
) AS seed(sort_order, icon, word)
WHERE NOT EXISTS (
    SELECT 1 FROM back_humeur_choices existing WHERE existing.word = seed.word
)
ORDER BY seed.sort_order;
