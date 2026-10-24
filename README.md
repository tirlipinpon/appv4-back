# appv4-back

Backend de l’application éducative **appv4** :
**Java 21 + Spring Boot 3.5.x**, architecture **hexagonale** (Ports & Adapters), API REST.

Détail des packages : **[ARCHITECTURE.md](./ARCHITECTURE.md)**.

Ce dépôt contient aussi l’environnement **Docker Compose** (PostgreSQL) pour le développement local.

## Prérequis

### Docker

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installé et **démarré**
- Vérifier :

```bash
docker version
docker compose version
```

### Java 21

- JDK 21 (ex. [Microsoft OpenJDK 21](https://learn.microsoft.com/java/openjdk/download))
- Vérifier :

```bash
java -version
```

Maven n’a **pas** besoin d’être installé globalement : le wrapper `mvnw` / `mvnw.cmd` est fourni.

```bash
.\mvnw.cmd -v
.\mvnw.cmd -DskipTests compile
```

### Lancer l’API (profil `local`)

1. Postgres Docker doit tourner (`docker compose up -d`)
2. Copier `.env.example` → `.env` et renseigner les secrets (ex. `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`)
3. Démarrer Spring **depuis la racine `appv4-back`** (le `.env` est chargé automatiquement) :

```bash
cd C:\Users\tonys\Desktop\appv4\appv4-back
.\mvnw.cmd spring-boot:run
```

Plus besoin d’exporter manuellement les variables du `.env` en PowerShell : `DotEnvEnvironmentPostProcessor` les injecte au boot. Les variables déjà définies dans l’OS / CI restent prioritaires.

4. Vérifier : [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) → `{"status":"UP"}`

Config : `application.yml` + `application-local.yml` (datasource JDBC) + `.env` local.
Le profil `local` n'est **pas** le défaut : il faut `SPRING_PROFILES_ACTIVE=local` (posé par le `.env`).
Sans profil, pas de seed admin ni Swagger UI.

## PostgreSQL local (MVP)

### 1. Variables d’environnement

```bash
# Depuis la racine de appv4-back
copy .env.example .env
```

Édite `.env` si besoin (mot de passe, port).  
**Ne committe jamais** le fichier `.env` (déjà dans `.gitignore`).

Valeurs par défaut (voir `.env.example`) :

| Variable | Défaut | Rôle |
|----------|--------|------|
| `POSTGRES_DB` | `appv4` | Nom de la base |
| `POSTGRES_USER` | `appv4` | Utilisateur |
| `POSTGRES_PASSWORD` | `change_me` | Mot de passe |
| `POSTGRES_PORT` | `5432` | Port sur ta machine |

### 2. Démarrer Postgres

```bash
cd C:\Users\tonys\Desktop\appv4\appv4-back
docker compose up -d
```

- `-d` = détaché (tourne en arrière-plan)
- Au **premier** lancement, Docker télécharge l’image `postgres:16-alpine` (un peu long)
- Postgres initialise la base (quelques secondes)

### 3. Vérifier que tout va bien

```bash
docker compose ps
```

Le service `postgres` doit être **healthy**.

Test connexion :

```bash
docker compose exec postgres pg_isready -U appv4 -d appv4
```

Attendu : `accepting connections`.

### 4. Logs

```bash
docker compose logs -f postgres
```

`Ctrl+C` pour quitter le suivi des logs (le conteneur continue de tourner).

### 5. Arrêter

```bash
docker compose down
```

Arrête le conteneur. Les **données** restent dans le volume Docker `appv4_pgdata`.

### 6. Reset complet (effacer les données)

```bash
docker compose down -v
```

`-v` supprime le volume → base vide au prochain `up`.  
Utile si tu veux repartir de zéro.

## Connexion depuis l’API (plus tard)

- Image / version : **Postgres 16** (`postgres:16-alpine`, serveur constaté 16.x)
- URL JDBC locale :

```text
jdbc:postgresql://localhost:5432/appv4
```

User / password = ceux du `.env`.  
Détail JDBC + variables `SPRING_DATASOURCE_*` : **[DATABASE.md](./DATABASE.md)**.

## Conventions (rappel ADR)

- Tables métier préfixées : `front_` (app learning) ou `back_` (admin)  
  → détail et exemples : **[DATABASE.md](./DATABASE.md)**
- API : `/api/ext/**` pour learning, `/api/int/**` pour admin
- CORS local (profil `local`) : `http://localhost:4200` et `http://localhost:4201`  
  → `appv4.cors.allowed-origins` dans `application-local.yml`

## Smoke hello

Avec Postgres + `.\mvnw.cmd spring-boot:run` :

- [http://localhost:8080/api/ext/hello](http://localhost:8080/api/ext/hello) → `{"text":"hello learning"}`
- [http://localhost:8080/api/int/hello](http://localhost:8080/api/int/hello) → `{"text":"admin-helloworld"}`
- OpenAPI (SCRUM-147) : [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- Swagger UI (profil `local` seulement) : [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Tickets liés

| Sujet | Jira |
|--------|------|
| Docker Compose parent | SCRUM-37 |
| README (cette doc) | SCRUM-45 |
| Suite : Postgres / Flyway | SCRUM-36 |
| Suite : Spring Boot hexa | SCRUM-35 |

## Prochaines étapes

1. Angular `learning` + `admin` consommant `/api/ext/hello` et `/api/int/hello` (SCRUM-34)
2. Erreurs ProblemDetail + OpenAPI (socle shared)

## Branches & deploiement

Voir **[BRANCHING.md](./BRANCHING.md)** : `feature/*` → **PR** → `develop` (CI) → **PR** → `release` (CD Azure).  
Important : le passage `develop` → `release` passe **toujours** par une Pull Request.

