#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091 # The shared file is resolved relative to this script at runtime.
source "$SCRIPT_DIR/prod-compose-common.sh"

if [[ "${RESTORE_CONFIRM:-}" != "yes" ]]; then
  echo "Restore replaces production database contents. Re-run with RESTORE_CONFIRM=yes after reviewing the dump path." >&2
  exit 2
fi
if [[ $# -ne 1 || ! -f "$1" || ! -s "$1" ]]; then
  echo "Usage: RESTORE_CONFIRM=yes $0 /path/to/non-empty-backup.dump" >&2
  exit 2
fi

prod_require_files
prod_compose config --quiet
database_id="$(prod_compose ps --status running --quiet db)"
if [[ -z "$database_id" ]]; then
  echo "The production database must be running before restore." >&2
  exit 1
fi

api_was_running=false
if [[ -n "$(prod_compose ps --status running --quiet api)" ]]; then
  api_was_running=true
  prod_compose stop api
fi

# shellcheck disable=SC2016 # These variables must expand inside the database container.
if ! prod_compose exec -T db sh -c 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec pg_restore --clean --if-exists --no-owner --no-privileges --exit-on-error --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"' <"$1"; then
  echo "Database restore failed. The API remains stopped if it was running." >&2
  exit 1
fi

if [[ "$api_was_running" == true ]]; then
  prod_compose start api
  prod_wait_for_health api
fi
echo "PostgreSQL restore completed from: $1"
