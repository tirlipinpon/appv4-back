# SCRUM-77 — Provision RG + Postgres Azure

## Objectif

Créer la fondation Azure **sans** déployer encore Spring ni les fronts :

1. Resource Group `rg-appv4-prod`
2. PostgreSQL Flexible Server `psql-appv4-prod` + database `appv4`
3. Règle firewall « Allow Azure services » (pour App Service, SCRUM-78)
4. User applicatif `back_app` (mdp hors git)

Référence ADR : [`ADR-001-azure-hosting.md`](ADR-001-azure-hosting.md).

## Prérequis

- Compte Azure + subscription active
- [Azure CLI](https://learn.microsoft.com/cli/azure/install-azure-cli) installé
- Connexion : `az login` puis `az account set --subscription "<nom-ou-id>"`

## Exécution (script)

Sous Git Bash / WSL :

```bash
cd appv4-back
export APPV4_PG_ADMIN_PASSWORD='…'   # admin serveur
export APPV4_PG_APP_PASSWORD='…'     # user back_app
export APPV4_ADMIN_IP="$(curl -s https://ifconfig.me)"  # optionnel, pour psql depuis ta machine
chmod +x infra/azure/provision-rg-postgres.sh
./infra/azure/provision-rg-postgres.sh
```

Sous PowerShell (équivalent manuel si Bash indispo) : voir section **Commandes `az`**.

## Commandes `az` (manuel)

```bash
az group create -n rg-appv4-prod -l francecentral

az postgres flexible-server create \
  -g rg-appv4-prod -n psql-appv4-prod -l francecentral \
  --admin-user pgadmin --admin-password "<ADMIN_PWD>" \
  --sku-name Standard_B1ms --tier Burstable \
  --storage-size 32 --version 16 \
  --database-name appv4 \
  --public-access None --yes

az postgres flexible-server firewall-rule create \
  -g rg-appv4-prod -n psql-appv4-prod \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 --end-ip-address 0.0.0.0
```

## Après provision

Renseigner les secrets GitHub (`docs/GITHUB_SECRETS.md`) — **jamais** dans le repo :

| Secret / var | Valeur |
|--------------|--------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://psql-appv4-prod.postgres.database.azure.com:5432/appv4?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `back_app` |
| `SPRING_DATASOURCE_PASSWORD` | mdp app |
| `AZURE_RESOURCE_GROUP` | `rg-appv4-prod` |

## Hors scope (tickets suivants)

| Ticket | Contenu |
|--------|---------|
| SCRUM-78 | App Service Java + connection string |
| SCRUM-79 | Static Web Apps learning + admin |
| SCRUM-80 | Brancher CD `cd-release.yml` |

Flyway : tables créées au **premier boot** du back sur Azure, pas pendant ce ticket.
