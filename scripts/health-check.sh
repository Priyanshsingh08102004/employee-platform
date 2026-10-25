#!/usr/bin/env bash
# Polls both services until they report healthy, or fails after a timeout. Used by deploy.sh and CI.
set -euo pipefail

EMPLOYEE_URL="${EMPLOYEE_URL:-http://localhost:8080/actuator/health}"
PAYROLL_URL="${PAYROLL_URL:-http://localhost:5100/health}"
TIMEOUT="${TIMEOUT:-120}"

wait_for() {
  local name="$1" url="$2" waited=0
  until curl -fsS "$url" >/dev/null 2>&1; do
    if (( waited >= TIMEOUT )); then
      echo "FAIL  $name did not become healthy within ${TIMEOUT}s ($url)" >&2
      return 1
    fi
    sleep 3
    waited=$(( waited + 3 ))
  done
  echo "OK    $name is healthy ($url)"
}

wait_for "employee-service (Java)" "$EMPLOYEE_URL"
wait_for "payroll-service  (.NET)" "$PAYROLL_URL"
echo "All services healthy."
