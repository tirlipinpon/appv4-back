#!/usr/bin/env bash
# SCRUM-77 — Provision Resource Group + Azure Database for PostgreSQL Flexible Server
# Prérequis : Azure CLI connecté (`az login`) + subscription active
# Usage :
#   export APPV4_PG_ADMIN_PASSWORD='…mot-de-passe-fort…'
#   ./infra/azure/provision-rg-postgres.sh
set -euo pipefail

LOCATION="${LOCATION:-francecentral}"
RG="${RG:-rg-appv4-prod}"
PG_SERVER="${PG_SERVER:-psql-appv4-prod}"
PG_DB="${PG_DB:-appv4}"
PG_ADMIN_USER="${PG_ADMIN_USER:-pgadmin}"
PG_APP_USER="${PG_APP_USER:-back_app}"
PG_SKU="${PG_SKU:-Standard_B1ms}"
PG_TIER="${PG_TIER:-Burstable}"
PG_VERSION="${PG_VERSION:-16}"
PG_STORAGE_GB="${PG_STORAGE_GB:-32}"

if [[ -z "${APPV4_PG_ADMIN_PASSWORD:-}" ]]; then
  echo "ERROR: exporte APPV4_PG_ADMIN_PASSWORD (mdp admin Postgres) avant d'exécuter." >&2
  exit 1
fi
if [[ -z "${APPV4_PG_APP_PASSWORD:-}" ]]; then
  echo "ERROR: exporte APPV4_PG_APP_PASSWORD (mdp user applicatif ${PG_APP_USER}) avant d'exécuter." >&2
  exit 1
fi

echo "==> Subscription active"
az account show --query "{name:name,id:id}" -o table

echo "==> Resource Group ${RG} (${LOCATION})"
az group create --name "${RG}" --location "${LOCATION}" -o table

echo "==> Postgres Flexible Server ${PG_SERVER} (peut prendre plusieurs minutes)"
# public access + firewall rules ajoutées ensuite (App Service / IP admin)
az postgres flexible-server create \
  --resource-group "${RG}" \
  --name "${PG_SERVER}" \
  --location "${LOCATION}" \
  --admin-user "${PG_ADMIN_USER}" \
  --admin-password "${APPV4_PG_ADMIN_PASSWORD}" \
  --sku-name "${PG_SKU}" \
  --tier "${PG_TIER}" \
  --storage-size "${PG_STORAGE_GB}" \
  --version "${PG_VERSION}" \
  --database-name "${PG_DB}" \
  --public-access "None" \
  --yes \
  -o table

echo "==> Autoriser Azure services (pour App Service SCRUM-78)"
az postgres flexible-server firewall-rule create \
  --resource-group "${RG}" \
  --name "${PG_SERVER}" \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0 \
  -o table

if [[ -n "${APPV4_ADMIN_IP:-}" ]]; then
  echo "==> Autoriser IP admin ${APPV4_ADMIN_IP}"
  az postgres flexible-server firewall-rule create \
    --resource-group "${RG}" \
    --name "${PG_SERVER}" \
    --rule-name AllowAdminIp \
    --start-ip-address "${APPV4_ADMIN_IP}" \
    --end-ip-address "${APPV4_ADMIN_IP}" \
    -o table
fi

FQDN=$(az postgres flexible-server show \
  --resource-group "${RG}" \
  --name "${PG_SERVER}" \
  --query fullyQualifiedDomainName -o tsv)

echo "==> Créer le rôle applicatif ${PG_APP_USER} (via az postgres flexible-server execute si dispo)"
# Note: création user app — via psql si l'IP admin est autorisée
if command -v psql >/dev/null 2>&1 && [[ -n "${APPV4_ADMIN_IP:-}" ]]; then
  export PGPASSWORD="${APPV4_PG_ADMIN_PASSWORD}"
  psql "host=${FQDN} port=5432 dbname=${PG_DB} user=${PG_ADMIN_USER} sslmode=require" <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${PG_APP_USER}') THEN
    CREATE ROLE ${PG_APP_USER} LOGIN PASSWORD '${APPV4_PG_APP_PASSWORD}';
  END IF;
END
\$\$;
GRANT ALL PRIVILEGES ON DATABASE ${PG_DB} TO ${PG_APP_USER};
SQL
else
  echo "INFO: psql indisponible ou APPV4_ADMIN_IP non défini."
  echo "      Crée le user ${PG_APP_USER} manuellement (Azure Portal Query Editor / psql) avec le mdp APPV4_PG_APP_PASSWORD."
fi

JDBC="jdbc:postgresql://${FQDN}:5432/${PG_DB}?sslmode=require"

cat <<EOF

========== SCRUM-77 terminé (infra) ==========
Resource Group : ${RG}
Postgres       : ${PG_SERVER}
FQDN           : ${FQDN}
Database       : ${PG_DB}
Admin user     : ${PG_ADMIN_USER}
App user       : ${PG_APP_USER}

Secrets GitHub (Environment production) à renseigner :
  SPRING_DATASOURCE_URL      = ${JDBC}
  SPRING_DATASOURCE_USERNAME = ${PG_APP_USER}
  SPRING_DATASOURCE_PASSWORD = (valeur de APPV4_PG_APP_PASSWORD)
  Variable AZURE_RESOURCE_GROUP = ${RG}

NE PAS committer ces mots de passe.
Flyway s'exécutera au premier démarrage App Service (SCRUM-78/80).
==============================================
EOF
