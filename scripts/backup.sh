#!/usr/bin/env bash
# Backs up the three Docker volumes (databases + uploaded files) into timestamped .tar.gz files.
set -euo pipefail
cd "$(dirname "$0")/.."

PROJECT="employee-platform"          # must match "name:" in docker-compose.yml
STAMP="$(date +%Y-%m-%d_%H%M%S)"
mkdir -p backups

for vol in employee-data employee-uploads payroll-data; do
  docker run --rm \
    -v "${PROJECT}_${vol}:/source:ro" \
    -v "$(pwd)/backups:/backup" \
    alpine tar czf "/backup/${vol}_${STAMP}.tar.gz" -C /source .
  echo "Backed up ${vol} -> backups/${vol}_${STAMP}.tar.gz"
done

# Keep only the 14 newest backups of each volume
for vol in employee-data employee-uploads payroll-data; do
  ls -1t backups/${vol}_*.tar.gz 2>/dev/null | tail -n +15 | xargs -r rm --
done
