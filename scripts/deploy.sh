#!/usr/bin/env bash
# One-command deploy: builds images, starts the stack, waits until everything is healthy.
set -euo pipefail
cd "$(dirname "$0")/.."

command -v docker >/dev/null || { echo "Docker is not installed or not on PATH" >&2; exit 1; }

if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "Created .env from .env.example - edit INTERNAL_API_KEY before deploying anywhere real."
fi

docker compose up -d --build
./scripts/health-check.sh
echo "Employee Manager: http://localhost:8080   (admin / admin123)"
