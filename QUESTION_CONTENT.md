# Questions et futurs jeux

Référence fonctionnelle : `../cursor_app_functionality_list.md`.

## Structure retenue

`back_subject_questions` conserve l'identité, la matière, la forme et l'énoncé.
Chaque structure possède son contenu et ses règles : aujourd'hui `choices` pour
QCM, `pairs` pour Memory, `items` pour Ordre, `statements` pour Vrai ou faux,
`links` pour Liens, et `sentence`, `answers`, `decoys` pour Case vide. Les tables existantes et leurs
identifiants sont conservés. Le nombre de tables n'est pas le facteur limitant ;
on évite surtout de lire les tables de tous les jeux pour chaque question.

Le cœur utilise trois contrats extensibles : `ContentInput` pour la saisie,
`DraftContent` après validation et `StoredContent` après persistance. Une future
image interactive peut ainsi transporter ses zones, et un puzzle ses pièces,
sans ajouter des paramètres à toutes les méthodes existantes.

- `QuestionFormHandler` valide une forme et fournit ses métadonnées.
- `QuestionFormRegistry` indexe les validateurs par code et construit le catalogue.
- `QuestionContentStore` lit, écrit et supprime le contenu propre à une forme.
- `TeacherQuestionService` gère les droits, la matière, la forme et la transaction.
- `TeacherQuestionPersistenceAdapter` charge les questions et délègue leurs contenus.

Seuls QCM, Memory, Ordre, Vrai ou faux, Liens et Case vide sont actuellement proposés par l'API.
Les interfaces de création et les moteurs des autres jeux restent à développer.

## Évolution vers les dix jeux

Ce tableau est une orientation de modélisation, pas la spécification définitive
de formats encore non implémentés.

| Jeu | Données spécifiques à prévoir | Situation |
| --- | --- | --- |
| QCM | Propositions, bonnes réponses, ordre | Implémenté : `choices` |
| Memory | Paires de faces associées | Implémenté : `pairs` |
| Chronologie | Éléments ordonnés, éventuellement dates | `order` couvre l'ordre simple ; dates à définir |
| Vrai/Faux | Phrases et côté vrai ou faux | Implémenté : `statements` |
| Liens | Mot de gauche et réponse de droite | Implémenté : `links` |
| Case vide | Phrase avec `___`, mot juste de chaque trou, leurres | Implémenté : `sentence`, `answers`, `decoys` |
| Simon | Séquence de stimuli et paramètres | Structure spécifique |
| Image interactive | Référence de média, zones, réponses | Structure spécifique |
| Réponse libre | Réponses admises et règles de comparaison | Structure spécifique |
| Puzzle | Référence de média, découpage, configuration | Structure spécifique |

Une table spécialisée convient aux données à relier, contraindre et rechercher.
Un contenu JSONB versionné pourra convenir à une configuration très variable,
dans le store concerné, avec validation explicite. Il n'est pas nécessaire de
convertir aujourd'hui toutes les données existantes en JSONB.

Le jeu jouable (scénario, configuration, score) et la forme d'une question sont
deux concepts distincts. Lors de l'implémentation des jeux, prévoir leur catalogue
et leurs formes compatibles ; une même structure pourra servir plusieurs jeux.
Ces futures tables de jeux ne sont pas créées prématurément par cette optimisation.

## Ajouter une forme

1. Définir ses types d'entrée, de contenu validé et de contenu enregistré.
2. Implémenter `QuestionFormHandler` et l'enregistrer comme bean dans la configuration.
3. Implémenter `QuestionContentStore` avec le même code de forme. Son chargement
   reçoit au plus 500 identifiants et retourne une entrée par question.
4. Ajouter une migration Flyway si cette structure exige un nouveau stockage.
5. Ajouter le DTO HTTP et le formulaire adaptés à la nouvelle structure. Les
   formes déjà publiées gardent leur contrat `choices` / `pairs` / `items`.
   Vrai ou faux ajoute `statements`, Liens ajoute `links`, Case vide ajoute
   `sentence`, `answers` et `decoys`.
   Les surcharges prenant `ContentInput` permettent aux nouveaux adaptateurs
   d'appeler le même service avec une structure différente.
6. Tester validation, droits, aller-retour en base et nombre de requêtes avant
   de proposer cette forme dans l'interface. Le moteur enfant est un travail distinct.

Le test `independentFormCarriesItsOwnStructureThroughTheCommonService` démontre
le passage d'un contenu d'image fictif sans modifier l'orchestration commune.
`QuestionContentBatchTest` vérifie l'extension du stockage et les lots de 500.

## Coûts et intégrité

La lecture coûte une requête pour les questions, puis une requête par forme
présente et par lot de 500 questions de cette forme. Une matière vide ne lance
aucune lecture de contenu. L'ordre de création des questions et l'ordre saisi
des éléments sont conservés.

Mesures vérifiées avec Hibernate sur PostgreSQL :

| Contenu de la matière | Ancien chargement | Nouveau chargement |
| --- | ---: | ---: |
| 100 questions QCM / Memory / Ordre | 301 requêtes | 4 requêtes |
| 100 QCM | 301 requêtes | 2 requêtes |
| Matière vide | 1 requête | 1 requête |

Ces comptes concernent les questions et leurs contenus, hors contrôle d'accès.
Ce sont des comptes SQL, pas des mesures de latence ou de capacité en production.
L'endpoint historique retourne encore toute la matière : une pagination sera
nécessaire si une matière contient des milliers de questions, pour limiter aussi
la mémoire et la taille de la réponse HTTP.

Les écritures n'effectuent plus un `flush` par élément. Les identifiants JPA
`IDENTITY` impliquent encore un INSERT par ligne : `saveAllAndFlush` ne constitue
pas à lui seul un batch JDBC d'INSERT. Les corrections utilisent un DELETE groupé
sur le contenu concerné et verrouillent la question pendant la transaction.

La création de la ligne matière/forme utilise `ON CONFLICT DO NOTHING` : deux
créations concurrentes ne provoquent plus une violation d'unicité récupérée dans
une transaction déjà en échec. La migration V33 ajoute la référence
question -> forme de la matière, interdit les positions négatives ou dupliquées
et remplace les anciens index de position par les index uniques correspondants.
Elle ne supprime aucun contenu. Elle est appliquée par Flyway au prochain démarrage.

## Vérification

`./mvnw.cmd verify` exécute Checkstyle et les tests métier/HTTP.
Les tests PostgreSQL sont activés seulement si `APPV4_PERSISTENCE_TEST_URL` est
défini. Utiliser une base **réservée aux tests** : Flyway y applique les migrations
et le test de montée de version y crée un schéma temporaire supplémentaire.

```powershell
$env:APPV4_PERSISTENCE_TEST_URL = 'jdbc:postgresql://localhost:5432/appv4_questions_test'
# Facultatifs : APPV4_PERSISTENCE_TEST_USER et APPV4_PERSISTENCE_TEST_PASSWORD
# Valeurs locales par défaut : appv4 / appv4.
.\mvnw.cmd verify
```

`TeacherQuestionPersistenceTest` mesure les lectures SQL et vérifie les mises à
jour et contraintes. `QuestionMigrationTest` crée des contenus en V32, migre en
V33 et vérifie les identifiants, contenus et suppressions en cascade.
