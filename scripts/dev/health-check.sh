#!/usr/bin/env bash

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

SERVICES=(
  "gateway:8080"
  "auth:8081"
  "catalog:8082"
  "order:8083"
  "inventory:8084"
  "cart:8085"
  "pricing:8086"
  "coupon:8087"
  "payment:8088"
  "shipping:8089"
  "notification:8091"
  "audit:8093"
)

SUCCESS=0
FAILED=0

echo ""
echo -e "${CYAN}Atlas Health Check${NC}"
echo -e "${CYAN}==================${NC}"
echo ""

for SERVICE in "${SERVICES[@]}"
do
    NAME=$(echo "$SERVICE" | cut -d':' -f1)
    PORT=$(echo "$SERVICE" | cut -d':' -f2)

    URL="http://localhost:${PORT}/actuator/health"

    RESPONSE=$(curl -s --max-time 5 "$URL")

    if echo "$RESPONSE" | grep -q '"status":"UP"'
    then
        echo -e "${GREEN}[UP]${NC}   ${NAME} (${PORT})"
        SUCCESS=$((SUCCESS+1))
    else
        echo -e "${RED}[FAIL]${NC} ${NAME} (${PORT})"
        FAILED=$((FAILED+1))
    fi
done

echo ""
echo -e "${CYAN}==================${NC}"
echo -e "${GREEN}Healthy services : ${SUCCESS}${NC}"
echo -e "${RED}Failed services  : ${FAILED}${NC}"
echo -e "${CYAN}==================${NC}"

if [ "$FAILED" -eq 0 ]
then
    echo ""
    echo -e "${GREEN}Atlas is fully operational.${NC}"
fi  