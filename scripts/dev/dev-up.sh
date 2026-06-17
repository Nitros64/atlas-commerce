#!/usr/bin/env bash
set -e

echo "Starting Atlas local dependencies..."

if ! docker version >/dev/null 2>&1; then
  echo "Docker is not running."
  exit 1
fi

start_or_create() {
  NAME="$1"
  IMAGE="$2"
  shift 2

  if docker ps -a --format "{{.Names}}" | grep -q "^${NAME}$"; then
    echo "Starting existing container: ${NAME}"
    docker start "${NAME}" >/dev/null
  else
    echo "Creating container: ${NAME}"
    docker run -d --name "${NAME}" "$@" "${IMAGE}" >/dev/null
  fi
}

start_or_create "atlas-redis" "redis:7" \
  -p 6379:6379 \
  redis-server --requirepass atlas-redis-pass

start_or_create "atlas-kafka" "apache/kafka:4.1.2" \
  -p 9092:9092

echo "Waiting for Kafka..."
sleep 15

start_or_create "atlas-rabbitmq" "rabbitmq:4-management" \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=atlas \
  -e RABBITMQ_DEFAULT_PASS=atlas

POSTGRES_IMAGE="postgres:17"

create_postgres() {
  NAME="$1"
  PORT="$2"
  DB="$3"

  start_or_create "$NAME" "$POSTGRES_IMAGE" \
    -e POSTGRES_DB="$DB" \
    -e POSTGRES_USER=atlas \
    -e POSTGRES_PASSWORD=atlas \
    -p "$PORT:5432"
}

create_postgres "atlas-postgres-auth" 5432 authdb
create_postgres "atlas-postgres-catalog" 5433 catalogdb
create_postgres "atlas-postgres-inventory" 5434 inventorydb
create_postgres "atlas-postgres-order" 5435 orderdb
create_postgres "atlas-postgres-payment" 5436 paymentdb
create_postgres "atlas-postgres-shipping" 5437 shippingdb
create_postgres "atlas-postgres-notification" 5438 notificationdb
create_postgres "atlas-postgres-audit" 5439 auditdb
create_postgres "atlas-postgres-coupon" 5440 coupondb
create_postgres "atlas-postgres-cart" 5441 cartdb

TOPICS=(
  "order-events"
  "order-events.DLT"
  "inventory-events"
  "inventory-events.DLT"
  "payment-events"
  "payment-events.DLT"
  "shipping-events"
  "shipping-events.DLT"
)

for TOPIC in "${TOPICS[@]}"; do
  PARTITIONS=3

  if [[ "$TOPIC" == *.DLT ]]; then
    PARTITIONS=1
  fi

  echo "Creating Kafka topic if missing: $TOPIC"

  docker exec atlas-kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 \
    --create \
    --if-not-exists \
    --topic "$TOPIC" \
    --partitions "$PARTITIONS" \
    --replication-factor 1 >/dev/null
done

echo ""
echo "Atlas local dependencies are ready."