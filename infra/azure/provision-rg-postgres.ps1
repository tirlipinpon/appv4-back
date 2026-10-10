# SCRUM-77 — Provision Resource Group + Postgres Flexible (PowerShell)
# Prérequis : az login
# Exemple :
#   $env:APPV4_PG_ADMIN_PASSWORD = '...'
#   $env:APPV4_PG_APP_PASSWORD   = '...'
#   $env:APPV4_ADMIN_IP = (Invoke-RestMethod https://ifconfig.me)
#   .\infra\azure\provision-rg-postgres.ps1

$ErrorActionPreference = "Stop"

$Location     = if ($env:LOCATION) { $env:LOCATION } else { "francecentral" }
$Rg           = if ($env:RG) { $env:RG } else { "rg-appv4-prod" }
$PgServer     = if ($env:PG_SERVER) { $env:PG_SERVER } else { "psql-appv4-prod" }
$PgDb         = if ($env:PG_DB) { $env:PG_DB } else { "appv4" }
$PgAdminUser  = if ($env:PG_ADMIN_USER) { $env:PG_ADMIN_USER } else { "pgadmin" }
$PgAppUser    = if ($env:PG_APP_USER) { $env:PG_APP_USER } else { "back_app" }
$PgSku        = if ($env:PG_SKU) { $env:PG_SKU } else { "Standard_B1ms" }
$PgTier       = if ($env:PG_TIER) { $env:PG_TIER } else { "Burstable" }
$PgVersion    = if ($env:PG_VERSION) { $env:PG_VERSION } else { "16" }
$PgStorageGb  = if ($env:PG_STORAGE_GB) { $env:PG_STORAGE_GB } else { "32" }

if (-not $env:APPV4_PG_ADMIN_PASSWORD) {
  throw "Exporte APPV4_PG_ADMIN_PASSWORD avant d'exécuter."
}
if (-not $env:APPV4_PG_APP_PASSWORD) {
  throw "Exporte APPV4_PG_APP_PASSWORD avant d'exécuter."
}

Write-Host "==> Subscription active"
az account show --query "{name:name,id:id}" -o table
if ($LASTEXITCODE -ne 0) { throw "az login requis" }

Write-Host "==> Resource Group $Rg ($Location)"
az group create --name $Rg --location $Location -o table

Write-Host "==> Postgres Flexible Server $PgServer (plusieurs minutes possibles)"
az postgres flexible-server create `
  --resource-group $Rg `
  --name $PgServer `
  --location $Location `
  --admin-user $PgAdminUser `
  --admin-password $env:APPV4_PG_ADMIN_PASSWORD `
  --sku-name $PgSku `
  --tier $PgTier `
  --storage-size $PgStorageGb `
  --version $PgVersion `
  --database-name $PgDb `
  --public-access None `
  --yes `
  -o table

Write-Host "==> Firewall AllowAzureServices (App Service SCRUM-78)"
az postgres flexible-server firewall-rule create `
  --resource-group $Rg `
  --name $PgServer `
  --rule-name AllowAzureServices `
  --start-ip-address 0.0.0.0 `
  --end-ip-address 0.0.0.0 `
  -o table

if ($env:APPV4_ADMIN_IP) {
  Write-Host "==> Firewall IP admin $($env:APPV4_ADMIN_IP)"
  az postgres flexible-server firewall-rule create `
    --resource-group $Rg `
    --name $PgServer `
    --rule-name AllowAdminIp `
    --start-ip-address $env:APPV4_ADMIN_IP `
    --end-ip-address $env:APPV4_ADMIN_IP `
    -o table
}

$fqdn = az postgres flexible-server show `
  --resource-group $Rg `
  --name $PgServer `
  --query fullyQualifiedDomainName -o tsv

$jdbc = "jdbc:postgresql://${fqdn}:5432/${PgDb}?sslmode=require"

Write-Host ""
Write-Host "========== SCRUM-77 terminé (infra) =========="
Write-Host "Resource Group : $Rg"
Write-Host "Postgres       : $PgServer"
Write-Host "FQDN           : $fqdn"
Write-Host "Database       : $PgDb"
Write-Host "Admin user     : $PgAdminUser"
Write-Host "App user       : $PgAppUser"
Write-Host ""
Write-Host "Secrets GitHub (Environment production) :"
Write-Host "  SPRING_DATASOURCE_URL      = $jdbc"
Write-Host "  SPRING_DATASOURCE_USERNAME = $PgAppUser"
Write-Host "  SPRING_DATASOURCE_PASSWORD = (APPV4_PG_APP_PASSWORD)"
Write-Host "  AZURE_RESOURCE_GROUP       = $Rg"
Write-Host ""
Write-Host "Creer le role $PgAppUser via Portal Query Editor / psql si besoin."
Write-Host "NE PAS committer les mots de passe."
Write-Host "=============================================="
