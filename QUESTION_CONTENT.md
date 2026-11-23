# Formes de questions et parcours

La question commune porte la matière, le code de forme et l’énoncé. Chaque forme valide et stocke ses données dans son propre `QuestionFormHandler` et `QuestionContentStore`. Le moteur `QuestionPlayEngine` produit les données jouables et corrige les réponses en s’appuyant sur le contenu enregistré.

| Forme | Saisie professeur | Soumission de jeu |
| --- | --- | --- |
| Choix multiples `qcm` | Propositions et bonnes réponses | Ensemble des choix sélectionnés |
| Vrai ou faux `vrai_faux` | Affirmations et verdict attendu | Booléen par affirmation |
| Memory `memory` | Paires de faces | Identifiants des deux cartes de chaque paire trouvée |
| Ordre `order` | Étapes ordonnées et jalons facultatifs de frise | Liste des éléments dans l’ordre retenu |
| Classement `classement` | Cartes et colonnes cibles | Colonne choisie par carte |
| Liens `liens` | Associations gauche et droite | Options choisies dans chaque colonne |
| Case vide `case_vide` | Phrase, mots attendus et leurres | Un jeton par trou |
| Image interactive `image_interactive` | Média, zones et ordre facultatif | Clics dans l’ordre réalisé |
| Lettre par lettre `reponse_libre` | Mot ou texte attendu | Saisie avec les accents et retour par case |
| Réponse libre `texte` | Texte attendu | Saisie comparée sans casse, accents ni espaces de bord |
| Puzzle `puzzle` | Média, pièces et plateau avec trous, blanc ou fond dédié | Coordonnées normalisées de chaque pièce |
| Simon `simon` | Genre et éléments | Positions rappelées sur la dernière manche |

Les réponses utilisent les identifiants des éléments proposés. Pour Liens et Case vide, deux jetons de même libellé peuvent être interchangés. Les leurres identiques aux réponses sont refusés à la saisie. Le score est un pourcentage. Une forme à réussite exacte réserve 100 % à une réponse entièrement juste.

## Parcours professeur

L’édition passe par `/api/int/teacher/subjects/{subjectId}/questions`. L’objet d’écriture est discriminé par `formCode` dans OpenAPI. L’aperçu d’une question enregistrée utilise `GET .../{questionId}/preview` et `POST .../{questionId}/preview/attempts`. Le moteur fournit le même rendu, la même correction et le même retour par case ou paire dans ce contexte de simulation. L’aperçu possède son propre tirage Simon. La simulation est en lecture seule pour les données de progression.

## Parcours enfant

`GET /api/ext/subjects/{subjectId}/questions` interroge uniquement l’identifiant, le code et l’énoncé via une projection. La page est ordonnée par identifiant. La valeur de `nextCursor` repart dans `afterId` pour la suite. La limite maximale est 200 et Learning demande 50 lignes. L’accès reste conditionné à une matière liée, visible et non exclue.

Une mission sélectionne la première question compatible par requête sur la matière entière. Le catalogue des formes jouables est partagé avec le moteur. Le plan du jour reste stable après sa création.

`POST .../{questionId}/play` ouvre la question. Le champ `content` dépend de `formCode`. Memory présente des cartes indépendantes et fait vérifier les deux cartes retournées par `POST .../memory-pair`. Liens présente deux listes indépendantes. Les médias sont chargés lors de l’ouverture d’une question illustrée.

`POST .../{questionId}/attempts` attend une seule variante de réponse. Learning fournit un `submissionId` et réutilise cet identifiant lors d’une relance. La réponse et ses effets sur la progression sont enregistrés ensemble. Une soumission rejouée retourne le même verdict.

Simon ajoute un `challengeId` à la réponse d’ouverture. L’enfant le renvoie avec sa tentative. Il identifie la suite affichée et appartient à la question et à l’enfant. Il expire après 30 minutes. Sa consommation est atomique avec l’enregistrement de la tentative. La simulation professeur utilise son propre champ `previewSequence`. Le GET historique et les soumissions historiques restent disponibles pour les clients antérieurs. Pour Simon, le support historique utilise un défi dédié à l’ancien protocole.

## Chargements et extension

Les lectures de contenu de l’éditeur regroupent les questions par forme et par lots de 500 identifiants. Les réponses de liste restent paginées. L’écriture délègue au store de la forme et sérialise les remplacements du contenu d’une même question.

Pour ajouter une forme, câbler le validateur, le store, la projection et le correcteur au moteur. Décrire ses champs dans le contrat HTTP et ajouter ses règles au rendu partagé du frontend. Couvrir l’aller-retour d’édition, la validation des réponses et la parité entre aperçu et jeu. Toute migration de schéma implique la revue des scripts de démo.

## Vérification

`./mvnw.cmd verify` exécute Checkstyle et les tests Java. Définir `APPV4_PERSISTENCE_TEST_URL` sur une base PostgreSQL dédiée pour inclure les tests de persistance. Le profil `postgres-integration` exécute aussi les tests de concurrence Failsafe.

```powershell
$env:APPV4_PERSISTENCE_TEST_URL = 'jdbc:postgresql://localhost:5432/appv4_questions_test'
.\mvnw.cmd -Ppostgres-integration verify
```

`OpenApiContractExportTest` exporte le contrat dans `target/openapi.json`. Reporter les changements dans `openapi/openapi.json` puis lancer `npm run api:generate` dans le frontend pour mettre à jour le schéma TypeScript partagé.
