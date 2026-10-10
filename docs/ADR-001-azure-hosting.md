# ADR-001 — Hébergement Azure appv4

- **Statut** : Accepté (SCRUM-76)
- **Date** : 2026-10-10
- **Contexte** : Déployer automatiquement depuis la branche `release` (CD GitHub Actions), sans déployer depuis `develop`.

## Décision

| Composant | Service Azure retenu | Pourquoi |
|-----------|----------------------|----------|
| Backend Spring (JAR Java 21) | **App Service** (Linux, runtime Java 21) | Déployer un JAR Spring Boot est simple (`az webapp deploy`) ; pas besoin de Docker pour le MVP |
| Front **learning** | **Static Web Apps** | SPA Angular statique ; token de deploy déjà prévu dans `docs/GITHUB_SECRETS.md` |
| Front **admin** | **Static Web Apps** (2ᵉ app) | Isolation learning / admin (ports 4200 / 4201 en local → 2 sites en prod) |
| Base de données | **Azure Database for PostgreSQL — Flexible Server** | Aligné avec Postgres 16 local / Flyway / JPA |
| Regroupement | **1 Resource Group** `rg-appv4-prod` | Un seul périmètre pour le MVP prod |
| Région | **France Central** (fallback West Europe) | Proximité ; changer uniquement si la région est indispo |

## Alternatives écartées (pour l’instant)

| Option | Pourquoi pas maintenant |
|--------|-------------------------|
| Container Apps / AKS pour le back | Utile plus tard ; impose image Docker + registry dès le MVP |
| App Service pour les fronts | Possible, mais SWA est plus adapté aux SPA + CI GitHub native |
| Cosmos DB / SQL Server | Le domaine et Flyway sont déjà sur PostgreSQL |
| 2 Resource Groups (front/back) | Surdimensionné pour un seul environnement prod |

## Conventions de nommage (provision SCRUM-77+)

| Ressource | Nom proposé |
|-----------|-------------|
| Resource Group | `rg-appv4-prod` |
| App Service plan | `plan-appv4-prod` |
| App Service (back) | `app-appv4-back-prod` |
| SWA learning | `swa-appv4-learning-prod` |
| SWA admin | `swa-appv4-admin-prod` |
| Postgres Flexible | `psql-appv4-prod` |
| Database | `appv4` |
| User applicatif | `back_app` (schémas / tables déjà préfixés `front_` / `back_` côté app) |

## Flux de déploiement (rappel)

```text
PR → develop (CI lint+test+build)
        ↓ PR
      release  →  CD GitHub Actions  →  Azure (prod)
```

- Trigger CD : push / merge sur `release` (workflow `cd-release.yml`)
- Deploy réel : seulement si `AZURE_DEPLOY_ENABLED=true` + secrets renseignés (SCRUM-75)
- Pas de deploy Azure depuis `develop` / `feature/*`

## Conséquences

- **SCRUM-77** : RG + Postgres Flexible + firewall (accès App Service)
- **SCRUM-78** : App Service Java 21 + settings datasource
- **SCRUM-79** : 2× Static Web Apps (learning, admin) + CORS back vers leurs URLs
- **SCRUM-80** : brancher les commandes deploy réelles dans `cd-release.yml` (retirer le placeholder)

## Liens

- Secrets : [`GITHUB_SECRETS.md`](GITHUB_SECRETS.md)
- Branches : [`../BRANCHING.md`](../BRANCHING.md)
