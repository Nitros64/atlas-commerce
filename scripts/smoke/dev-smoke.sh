#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:18080}"
BASE_URL="${BASE_URL%/}"
SMOKE_ATTEMPTS="${SMOKE_ATTEMPTS:-5}"
SMOKE_INTERVAL_SECONDS="${SMOKE_INTERVAL_SECONDS:-2}"

if ! command -v curl >/dev/null 2>&1; then
  echo "ERROR: curl is required." >&2
  exit 1
fi

if ! [[ "$SMOKE_ATTEMPTS" =~ ^[1-9][0-9]*$ ]]; then
  echo "ERROR: SMOKE_ATTEMPTS must be a positive integer." >&2
  exit 1
fi

if ! [[ "$SMOKE_INTERVAL_SECONDS" =~ ^[0-9]+$ ]]; then
  echo "ERROR: SMOKE_INTERVAL_SECONDS must be a non-negative integer." >&2
  exit 1
fi

SMOKE_TMP_DIR="$(mktemp -d)"
trap 'rm -rf -- "$SMOKE_TMP_DIR"' EXIT

request_until_match() {
  local name="$1"
  local path="$2"
  local expected_pattern="$3"
  local response_file="$SMOKE_TMP_DIR/response"
  local status_file="$SMOKE_TMP_DIR/status"
  local attempt status body

  for ((attempt = 1; attempt <= SMOKE_ATTEMPTS; attempt++)); do
    status="$({
      curl --silent --show-error \
        --connect-timeout 2 \
        --max-time 5 \
        --output "$response_file" \
        --write-out '%{http_code}' \
        "$BASE_URL$path"
    } 2>"$status_file" || true)"
    if [[ -f "$response_file" ]]; then
      body="$(<"$response_file")"
    else
      body=""
    fi

    if [[ "$status" == "200" ]] \
      && [[ -f "$response_file" ]] \
      && grep -Eq "$expected_pattern" "$response_file"; then
      echo "PASS: $name ($path)"
      return 0
    fi

    if ((attempt < SMOKE_ATTEMPTS)); then
      sleep "$SMOKE_INTERVAL_SECONDS"
    fi
  done

  echo "ERROR: $name failed after $SMOKE_ATTEMPTS attempt(s)." >&2
  echo "URL: $BASE_URL$path" >&2
  echo "HTTP status: ${status:-unavailable}" >&2
  if [[ -s "$status_file" ]]; then
    echo "curl: $(<"$status_file")" >&2
  fi
  echo "Response: ${body:-<empty>}" >&2
  return 1
}

echo "Running Atlas DEV HTTP smoke against $BASE_URL"

request_until_match \
  "gateway readiness" \
  "/actuator/health/readiness" \
  '"status"[[:space:]]*:[[:space:]]*"UP"'

request_until_match \
  "gateway-to-auth route" \
  "/api/v1/auth/ping" \
  '^auth-service up$'

echo "Atlas DEV HTTP smoke passed."
