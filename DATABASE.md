# Base de données appv4 — conventions

## Plateforme (versions alignées)

| Élément | Valeur figée pour le MVP local |
|---------|--------------------------------|
| Image Docker | `postgres:16-alpine` (`docker-compose.yml`) |
| Version serveur constatée | **PostgreSQL 16.15** (`postgres --version` dans le conteneur) |
| Encodage | UTF-8 |
| Migrations | Flyway (à brancher avec Spring Boot — SCRUM-48) |

**Règle :** Docker, la doc et Spring doivent toujours parler de la **même** famille Postgres 16.  
Si tu changes l’image Compose, mets à jour ce fichier + la config Spring.

## Connexion locale

Voir [README.md](./README.md) et le fichier `.env` (copié depuis `.env.example`).

### Paramètres

```text
Host:     localhost
Port:     5432          # ou POSTGRES_PORT du .env
Database: appv4         # POSTGRES_DB
User:     appv4         # POSTGRES_USER
Password: (voir .env)   # POSTGRES_PASSWORD — ne jamais committer
```

### URL JDBC (Spring Boot)

Format standard PostgreSQL :

```text
jdbc:postgresql://localhost:5432/appv4
```

Si tu changes le port dans `.env` (ex. `5433`) :

```text
jdbc:postgresql://localhost:5433/appv4
```

### Variables Spring recommandées (plus tard)

À utiliser dans `application-local.yml` / env (sans secrets en git) :

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/appv4
SPRING_DATASOURCE_USERNAME=appv4
SPRING_DATASOURCE_PASSWORD=...   # même valeur que POSTGRES_PASSWORD
SPRING_PROFILES_ACTIVE=local
```

Driver JDBC : `org.postgresql.Driver` (dépendance `org.postgresql:postgresql` via Spring Boot).

## Convention de nommage des tables (OBLIGATOIRE)

Toute table **métier** doit commencer par l’un de ces deux préfixes :

| Préfixe | Usage | Consommée surtout par |
|---------|--------|------------------------|
| `front_` | Données de l’expérience **learning** (enfant) : progression, tentatives, badges côté enfant, messages “hello” MVP, etc. | App Angular `learning` via API `/api/ext/**` |
| `back_` | Données **admin** : comptes, écoles, matières, jeux pédagogiques, affectations, messages admin MVP, etc. | App Angular `admin` via API `/api/int/**` |

### Pourquoi ?

- On lit tout de suite **à qui** appartient la donnée (enfant vs admin).
- Ça évite de mélanger les modèles dans le même “sac” sans règle.
- Les juniors savent où créer une nouvelle table sans débat.

### Exemples valides

```text
front_hello_messages
front_game_attempts
front_child_badges

back_hello_messages
back_subjects
back_teachers
back_games
```

### Exemples interdits

```text
hello_messages      ❌ pas de préfixe
users               ❌ pas de préfixe
game_attempts       ❌ ambigu (front ou back ?)
frontend_attempts   ❌ mauvais préfixe (utiliser front_)
```

### Tables techniques Flyway

Les tables gérées par l’outil de migration (ex. `flyway_schema_history`) **n’ont pas** besoin du préfixe `front_` / `back_` : ce ne sont pas des tables métier applicatives.

## MVP smoke (migration V1)

Migration : `db/migration/V1__create_hello_messages.sql`

- `front_hello_messages` — seed : `hello learning` → `GET /api/ext/hello`
- `back_hello_messages` — seed : `admin-helloworld` → `GET /api/int/hello`

## Checklist avant de créer une table

1. Est-ce une donnée **learning** ou **admin** ?
2. Préfixe `front_` ou `back_` choisi ?
3. Migration Flyway ajoutée (pas de CREATE TABLE manuel en prod) ?
4. L’API qui l’expose est-elle sous `/api/ext` (front) ou `/api/int` (back) ?

## Tickets liés

| Sujet | Jira |
|--------|------|
| Parent Postgres | SCRUM-36 |
| Cette doc | SCRUM-46 |
| JDBC / version | SCRUM-47 |
| Flyway | SCRUM-48 |
| Migration V1 hello | SCRUM-49 |
