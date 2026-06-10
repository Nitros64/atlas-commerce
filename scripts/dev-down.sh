#!/usr/bin/env bash

echo "Stopping Atlas infrastructure..."

docker compose \
  -f ./platform/docker/docker-compose.dev.yml \
  down

echo ""
echo "Atlas infrastructure stopped."