# Architecture hexagonale — appv4-back

Référence générale : `architecture-hexagonale-nouveau-projet.md` (racine `appv4`).  
Ce fichier décrit **l’adaptation appv4** (features, `/api/ext` vs `/api/int`, `front_` / `back_`, JPA).

## Idée simple

Monolithe modulaire découpé **par fonctionnalité**.  
Le domaine et l’application ne connaissent ni HTTP, ni Spring Data, ni Postgres.

```text
Adaptateur entrant              Coeur                         Adaptateur sortant

HTTP / REST
    |
    v
Controller ---> Port in (UseCase) ---> Service applicatif
                                            |
                                            +---> Domaine
                                            |
                                            +---> Port out <--- PersistenceAdapter ---> JPA ---> PostgreSQL

                        Configuration Spring = câblage
```

## Organisation des packages

```text
com.appv4
|-- Appv4BackApplication.java
|-- configuration/                 Configuration transversale
|-- shared/
|   |-- domain/                    DomainException, ErrorCode
|   `-- infrastructure/            (erreurs HTTP, OpenAPI, CORS — à venir)
`-- auth/                           Fonctionnalites metier et adaptateurs
    |-- application/                Services et ports
    `-- infrastructure/             Controleurs et persistance
```

Une feature ne contient que ce dont elle a besoin. Pas de dossiers vides “pour la forme” au-delà des placeholders MVP.

## Règles de dépendance

1. **Domaine** : zéro Spring, zéro JPA, zéro HTTP.
2. **Application** : dépend du domaine ; parle aux **ports** uniquement.
3. **Infrastructure** : dépend de l’application et du domaine ; jamais l’inverse.
4. Un **controller** appelle un **port entrant** (UseCase), jamais un repository JPA.
5. Un **service applicatif** utilise un **port sortant**, jamais `EntityManager` / `JpaRepository` directement.
6. Une **entité JPA** n’est pas un objet du domaine.

## Mapping API & tables (spécifique appv4)

| App Angular | Préfixe HTTP | Package controllers |
|-------------|--------------|---------------------|
| `learning` | `/api/ext/**` | Reserve pour les API learning |
| `admin` | `/api/int/**` | `auth.infrastructure.adapter.in.web` |

| Données DB | Préfixe table |
|------------|---------------|
| learning | `front_` |
| admin | `back_` |

> Le sous-package s’appelle `intapi` car `int` est un mot réservé Java. Les URLs restent `/api/int/**`.

## Persistance

- **JPA** (`spring-boot-starter-data-jpa`) + **Flyway** (autorité du schéma).
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate ne crée pas les tables.
- Forme attendue par feature : `Entity` → `JpaRepository` → `PersistenceAdapter` (impl du port out) → `PersistenceMapper`.

## Point d’entrée

`com.appv4.Appv4BackApplication` scanne tout le sous-arbre `com.appv4`.
