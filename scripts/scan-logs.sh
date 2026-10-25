#!/usr/bin/env bash
# Quick triage: shows errors and exceptions from the last hour across both services.
set -euo pipefail
cd "$(dirname "$0")/.."

SINCE="${1:-1h}"
echo "== ERROR / Exception lines in the last ${SINCE} =="
docker compose logs --no-color --since "$SINCE" | grep -Ei 'error|exception|fail' || echo "(none found)"
