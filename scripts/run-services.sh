#!/usr/bin/env bash

set -e

echo "===================================="
echo "Starting Atlas microservices"
echo "===================================="

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

start_service() {

    NAME=$1
    PATH_SERVICE=$2
    PORT=$3

    echo ""
    echo "Starting ${NAME}..."

    cd "${ROOT_DIR}/${PATH_SERVICE}"

    SPRING_PROFILES_ACTIVE=local \
    SERVER_PORT=${PORT} \
    nohup mvn spring-boot:run \
        > "${ROOT_DIR}/logs/${NAME}.log" 2>&1 &

    echo "${NAME} started"
}

mkdir -p "${ROOT_DIR}/logs"

start_service auth-service         services/auth-service         8081
sleep 3

start_service catalog-service      services/catalog-service      8082
sleep 3

start_service order-service        services/order-service        8083
sleep 3

start_service inventory-service    services/inventory-service    8084
sleep 3

start_service cart-service         services/cart-service         8085
sleep 3

start_service pricing-service      services/pricing-service      8086
sleep 3

start_service coupon-service       services/coupon-service       8087
sleep 3

start_service payment-service      services/payment-service      8088
sleep 3

start_service shipping-service     services/shipping-service     8089
sleep 3

start_service notification-service services/notification-service 8091
sleep 3

start_service audit-service        services/audit-service        8093
sleep 3

start_service gateway-service      services/gateway-service      8080

echo ""
echo "===================================="
echo "Atlas startup completed"
echo "===================================="
echo ""

echo "Logs directory:"
echo "${ROOT_DIR}/logs"