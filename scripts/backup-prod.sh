#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091 # The shared file is resolved relative to this script at runtime.
source "$SCRIPT_DIR/prod-compose-common.sh"

prod_require_files
prod_compose config --quiet

container_id="$(prod_compose ps --all --quiet db)"
if [[ -z "$container_id" ]]; then
  echo "No production database exists yet; skipping the initial deployment backup."
  exit 0
fi

running_id="$(prod_compose ps --status running --quiet db)"
if [[ -z "$running_id" ]]; then
  echo "The production database container exists but is not running; refusing to deploy without a backup." >&2
  exit 1
fi

mkdir -p "$PROD_BACKUP_DIR"
chmod 700 "$PROD_BACKUP_DIR"
umask 077
timestamp="$(date -u +%Y%m%dT%H%M%SZ)-$$"
destination="$PROD_BACKUP_DIR/open-community-${timestamp}.dump"
temporary="$(mktemp "$PROD_BACKUP_DIR/.open-community-${timestamp}.XXXXXX.tmp")"
trap 'rm -f "$temporary"' EXIT

# shellcheck disable=SC2016 # These variables must expand inside the database container.
if ! prod_compose exec -T db sh -c 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec pg_dump --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --no-password --format=custom' >"$temporary"; then
  echo "PostgreSQL backup failed; no deployment was attempted." >&2
  exit 1
fi
if [[ ! -s "$temporary" ]]; then
  echo "PostgreSQL returned an empty backup; no deployment was attempted." >&2
  exit 1
fi

chmod 600 "$temporary"
mv -- "$temporary" "$destination"
find "$PROD_BACKUP_DIR" -maxdepth 1 -type f -name 'open-community-*.dump' -mmin +20160 -delete
trap - EXIT
echo "Created PostgreSQL backup: $destination"
