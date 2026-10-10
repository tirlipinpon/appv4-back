# Stratégie de branches — appv4-back

## Branches

| Branche | Rôle |
|---------|------|
| `main` | Historique stable / miroir des releases validées |
| `develop` | Intégration continue (développement) |
| `release` / `release/x.y` | Cible de release ; le merge (via PR) déclenche le **CD Azure** |
| `feature/*` | Travail court, merge **via PR** vers `develop` |

## Flux (toujours via Pull Request)

```text
feature/*  --PR(+CI)-->  develop  --PR(+CI)-->  release  --CD-->  Azure
                              ^                     ^
                              |                     |
                         CI obligatoire        CI obligatoire
                                               (puis deploy Azure)
```

1. Développer sur `feature/...`
2. Ouvrir une **PR** vers **`develop`** → CI GitHub Actions (obligatoire)
3. Pour publier : ouvrir une **PR** **`develop` → `release`** (ou `release/x.y`) — **jamais de merge direct en ligne de commande / push direct**
4. Le merge de cette PR sur `release` déclenche le **CD Azure** (SCRUM-38 / SCRUM-39)

## Règles (non négociables)

- **Toute** intégration vers `develop`, `release` ou `main` passe par une **Pull Request**
- Interdit : `git push origin develop` / `git push origin release` avec du travail direct
- Interdit : merger `develop` dans `release` en local puis push sans PR
- Ne **pas** déployer Azure depuis `develop` ou une `feature/*`
- CI rouge = pas de merge

## Protections GitHub (obligatoires) — déjà activées

Sur **`develop`** et **`release`** (ruleset + branch protection) :

- Merge **uniquement via PR** (pas de push direct)
- Status check obligatoire : **`lint-test-build`** (Checkstyle + tests + package)
- CI rouge ⇒ bouton merge bloqué
- Pas de force-push / suppression de branche

## CI actuelle (gate avant merge humain)

Workflow : `.github/workflows/ci-develop.yml`  
Triggers : `push` / `pull_request` vers **`develop`**, et `pull_request` vers **`release`** / **`release/**`.

Obligatoire (CI rouge = **pas de merge**) :
1. **Lint** — Checkstyle (`./mvnw checkstyle:check`)
2. **Tests** — `./mvnw test`
3. **Package** — `./mvnw package`

> En Java, le « linter » standard côté Maven est **Checkstyle** (style / imports / naming). SpotBugs / PMD existent aussi pour la qualité, hors scope actuel.

## CD release (squelette)

Workflow : `.github/workflows/cd-release.yml`  
Triggers : `push` sur **`release`** / **`release/**`** (et `workflow_dispatch`).  
Jobs : `build` (tests + package + artefact JAR) puis `deploy` (Environment GitHub `production`) **uniquement** si `vars.AZURE_DEPLOY_ENABLED=true`.

Secrets / variables : voir [`docs/GITHUB_SECRETS.md`](docs/GITHUB_SECRETS.md) (**SCRUM-75**).  
Choix Azure (ADR) : [`docs/ADR-001-azure-hosting.md`](docs/ADR-001-azure-hosting.md) (**SCRUM-76**).  
Provision Azure réelle : **SCRUM-77+**.

Ticket : **SCRUM-71** — CD : **SCRUM-74** / secrets : **SCRUM-75** / Azure ADR : **SCRUM-76** / provision : **SCRUM-77+**.
