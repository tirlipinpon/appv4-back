# Données de démonstration pour le front

Depuis `appv4-back`, avec Docker Compose démarré et Python 3 installé :

```powershell
python scripts/front-demo/demo.py apply     # Installer (refuse si déjà présent)
python scripts/front-demo/demo.py status    # Volumes et formes
python scripts/front-demo/demo.py verify    # Vérifier les relations et contenus
python scripts/front-demo/demo.py rollback  # Supprimer le jeu de démonstration
python scripts/front-demo/demo.py reset     # Supprimer puis recréer en une transaction
```

Le script cible uniquement le service PostgreSQL du Compose de ce dépôt. Aucun
serveur distant, aucune variable JDBC, aucun envoi d'e-mail. Le backend doit avoir
appliqué les migrations jusqu'à V47. Une nouvelle version de schéma exige une
revue du générateur avant de modifier sa garde. Ce n'est pas une migration Flyway
ni un seed exécuté au démarrage.

## Contenu

- 12 écoles fictives marquées `[DEMO]`, 6 françaises et 6 belges.
- 186 couples école/année : maternelle, primaire, collège/secondaire et lycée.
- 360 professeurs approuvés, 450 affectations à des écoles, 4 500 années cochées.
- 240 comptes parents et 30 professeurs également parents : 270 familles,
  chacune de 1 à 5 enfants, soit 810 enfants.
- 654 matières actives : 2 à 5 par école/année, plus 42 archives masquées ou désactivées.
- 196 550 questions, de 200 à 400 par matière active, réparties entre les dix
  formes enregistrées dans le backend. Les archives sont volontairement vides.
- Partages entre professeurs, accès désactivés, invitations share/move/ask/handover,
  historiques acceptés/refusés/annulés et notifications ouvertes ou fermées.
- Fratries, demi-fratries, cousins, jumeaux, écoles et années identiques ou différentes,
  quelques enfants sans école, masqués ou désactivés, matières scolaires exclues
  et matières supplémentaires hors classe.
- Noms accentués et longs, six avatars, énoncés courts, longs ou facultatifs vides,
  tailles de réponses variables, QCM à réponses simples/multiples, images WebP valides.

Les contenus sont des exemples synthétiques pour tester l'affichage et les relations,
pas une banque d'exercices pédagogiques adaptée à chaque matière et à chaque âge.
Les données et les distributions sont reproductibles ; les identifiants et dates
peuvent changer après réinstallation. Les séquences PostgreSQL ne sont jamais remises
à zéro.

## Se connecter

Mot de passe commun des comptes de démonstration : **`FrontDemo2026!`**.
PIN des enfants : **`1234`**. Seuls les hashes BCrypt sont enregistrés en base.

| Compte | Parcours |
| --- | --- |
| `teacher001@front-demo.example.test` | Professeur, matières partagées et invitations |
| `teacher012@front-demo.example.test` | Professeur multi-écoles, également parent |
| `parent001@front-demo.example.test` | Famille avec un enfant |
| `parent005@front-demo.example.test` | Famille avec cinq enfants |
| `parent010@front-demo.example.test` | Famille avec jumeaux |

Tous les comptes `teacher001` à `teacher360`, et `parent001` à `parent240`, existent
sur le domaine `front-demo.example.test`. Utiliser le front admin qui porte les
espaces parent/professeur (`/parent` et `/teacher`). Aucun compte admin existant
n'est modifié.

## Suppression et sécurité des données existantes

Le schéma séparé `dev_front_demo` conserve les identifiants des racines créées.
Le rollback utilise ces identifiants, jamais une recherche par nom ou par e-mail.
Il supprime également les enfants et matières ajoutés aux comptes de démonstration
pendant les essais, ainsi que leurs questions et liens dépendants.

Les tables métier sont verrouillées en écriture pendant la suppression, et toutes
les opérations sont transactionnelles. En cas d'erreur, rien n'est partiellement
supprimé ou installé. `reset` annule aussi la suppression si la recréation échoue.
Les commandes concurrentes sont sérialisées par un verrou PostgreSQL.

Si un vrai compte/enfant/matière a été lié au jeu de démonstration depuis le front,
le rollback refuse la suppression. Retirer ces liens dans l'app puis relancer.
Les modifications réalisées sur les comptes de démonstration sont perdues lors
du rollback/reset. Les copies de questions faites vers une matière réelle ne sont
pas traçables et restent dans cette matière réelle.

Un petit schéma de suivi vide reste après rollback. Ne pas le supprimer lorsque
le jeu est installé : il permet d'identifier les données à retirer.

## Vérification du cycle complet

```powershell
python scripts/front-demo/test_lifecycle.py
```

Ce test réinitialise uniquement le jeu de démonstration et le laisse installé.
Il vérifie l'insertion, le refus d'une deuxième insertion, la protection contre
les liens vers un compte extérieur, les suppressions répétées, la réinstallation
et les invariants métier. Il compare les nombres de lignes et les empreintes du
contenu complet de toutes les tables `back_*` avant/après le cycle. Éviter de
modifier des données dans le front pendant ce test pour garder la comparaison stable.

L'image `shapes.webp` est une illustration géométrique locale de deux moitiés
colorées utilisée par les formes image interactive et puzzle. Aucune dépendance
Python supplémentaire n'est nécessaire pour exécuter les commandes.
