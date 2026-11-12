# Données de démo = schéma courant

Le jeu local est `scripts/front-demo/`
(`python scripts/front-demo/demo.py`). `apply.sql` refuse l'installation si la
dernière migration Flyway réussie n'est pas la version gravée dans la garde
(aujourd'hui **V47**).

## Dès qu'une migration change le schéma

Table, colonne, contrainte, check, enum, NOT NULL ou clé étrangère sur une
table `back_*` (ou une table que le jeu devra remplir) :

1. **`apply.sql`** — colonnes des `INSERT`, valeurs qui respectent les nouvelles
   contraintes, puis la garde `flyway_schema_history` sur la **nouvelle** version.
2. **`verify.sql`** — invariants et formes de questions si le métier change.
3. **`rollback.sql`** — nouveau lien qui peut mélanger un compte démo et un vrai compte.
4. **`status.sql`** et **`scripts/front-demo/README.md`** — version de schéma,
   volumes si les quantités changent.

Une colonne obligatoire sans valeur, ou une colonne supprimée encore citée,
fait échouer `apply`. Ne pas seulement changer le numéro dans la garde.

## Ne pas

- Avancer la garde sans relire les `INSERT`.
- Inventer une colonne ou une forme absente de la migration.
- Modifier une migration Flyway déjà appliquée : nouvelle migration, puis le script.

## Contrôle

Postgres du Compose démarré, depuis la racine de ce dépôt :

```powershell
python scripts/front-demo/demo.py reset
python scripts/front-demo/test_lifecycle.py
```

`reset` doit passer. Le test de cycle compare toutes les tables `back_*`.
