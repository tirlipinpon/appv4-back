# SCRUM-80 — CD automatique depuis `release`

## Flux

```text
PR feature → develop (CI)
PR develop → release  →  push release  →  CD (build + deploy Azure)
```

## Backend (`appv4-back`)

Workflow : `.github/workflows/cd-release.yml`

1. Lint Checkstyle + tests + package JAR  
2. Si `vars.AZURE_DEPLOY_ENABLED=true` → `az webapp deploy` vers App Service

### Variables / secrets GitHub (Environment `production`)

| Type | Nom | Valeur actuelle |
|------|-----|-----------------|
| var | `AZURE_DEPLOY_ENABLED` | `true` |
| var | `AZURE_RESOURCE_GROUP` | `rg-appv4-prod` |
| var | `AZURE_WEBAPP_NAME` | `app-appv4-back-prod` |
| secret | `AZURE_CREDENTIALS` | JSON service principal (Contributor sur le RG) |

Les settings datasource restent sur l’App Service (pas forcément rejoués à chaque CD).

## Frontend (`appv4-front`)

1. Lint + tests + build learning/admin  
2. Si `AZURE_DEPLOY_ENABLED=true` → upload vers les 2 Static Web Apps

| Type | Nom |
|------|-----|
| var | `AZURE_DEPLOY_ENABLED` = `true` |
| secret | `AZURE_SWA_LEARNING_TOKEN` |
| secret | `AZURE_SWA_ADMIN_TOKEN` |

## Premier run

Après merge de cette config sur `develop`, ouvrir une **PR `develop` → `release`** (humain) : le merge déclenche le CD.
