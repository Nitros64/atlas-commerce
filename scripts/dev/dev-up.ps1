Write-Host "Starting Atlas local dependencies..." -ForegroundColor Cyan

docker version | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker Desktop is not running." -ForegroundColor Red
    exit 1
}

function Start-Or-Create {
    param (
        [string]$Name,
        [string]$Image,
        [string[]]$Args,
        [int]$WaitSeconds = 0
    )

    $exists = docker ps -a --format "{{.Names}}" | Select-String -Pattern "^$Name$"

    if ($exists) {
        Write-Host "Starting existing container: $Name" -ForegroundColor Yellow
        docker start $Name | Out-Null
    } else {
        Write-Host "Creating container: $Name" -ForegroundColor Green
        docker run -d --name $Name @Args $Image | Out-Null
    }

    if ($WaitSeconds -gt 0) {
        Start-Sleep -Seconds $WaitSeconds
    }
}

Start-Or-Create -Name "atlas-redis" -Image "redis:7" -Args @(
    "-p", "6379:6379",
    "redis-server", "--requirepass", "atlas"
)

Start-Or-Create -Name "atlas-kafka" -Image "apache/kafka:4.1.2" -Args @(
    "-p", "9092:9092"
) -WaitSeconds 15

Start-Or-Create -Name "atlas-rabbitmq" -Image "rabbitmq:4-management" -Args @(
    "-p", "5672:5672",
    "-p", "15672:15672",
    "-e", "RABBITMQ_DEFAULT_USER=atlas",
    "-e", "RABBITMQ_DEFAULT_PASS=atlas"
)

$postgresImage = "postgres:17"

$databases = @(
    @{ Name = "atlas-postgres-auth";         Port = "5432"; Db = "authdb" },
    @{ Name = "atlas-postgres-catalog";      Port = "5433"; Db = "catalogdb" },
    @{ Name = "atlas-postgres-inventory";    Port = "5434"; Db = "inventorydb" },
    @{ Name = "atlas-postgres-order";        Port = "5435"; Db = "orderdb" },
    @{ Name = "atlas-postgres-payment";      Port = "5436"; Db = "paymentdb" },
    @{ Name = "atlas-postgres-shipping";     Port = "5437"; Db = "shippingdb" },
    @{ Name = "atlas-postgres-notification"; Port = "5438"; Db = "notificationdb" },
    @{ Name = "atlas-postgres-audit";        Port = "5439"; Db = "auditdb" },
    @{ Name = "atlas-postgres-coupon";       Port = "5440"; Db = "coupondb" },
    @{ Name = "atlas-postgres-cart";         Port = "5441"; Db = "cartdb" }
)

foreach ($db in $databases) {
    Start-Or-Create -Name $db.Name -Image $postgresImage -Args @(
        "-e", "POSTGRES_DB=$($db.Db)",
        "-e", "POSTGRES_USER=atlas",
        "-e", "POSTGRES_PASSWORD=atlas",
        "-p", "$($db.Port):5432"
    )
}

$topics = @(
    "order-events",
    "order-events.DLT",
    "inventory-events",
    "inventory-events.DLT",
    "payment-events",
    "payment-events.DLT",
    "shipping-events",
    "shipping-events.DLT"
)

foreach ($topic in $topics) {
    $partitions = 3
    if ($topic.EndsWith(".DLT")) {
        $partitions = 1
    }

    Write-Host "Creating Kafka topic if missing: $topic" -ForegroundColor Green

    docker exec atlas-kafka /opt/kafka/bin/kafka-topics.sh `
      --bootstrap-server localhost:9092 `
      --create `
      --if-not-exists `
      --topic $topic `
      --partitions $partitions `
      --replication-factor 1 | Out-Null
}

Write-Host ""
Write-Host "Atlas local dependencies are ready." -ForegroundColor Green