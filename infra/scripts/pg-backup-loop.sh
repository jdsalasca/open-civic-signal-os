#!/bin/sh
set -eu

: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${PGPASSWORD:?PGPASSWORD is required}"
: "${PGHOST:=db}"
: "${PGPORT:=5432}"
: "${BACKUP_HOUR_UTC:=2}"

case "$BACKUP_HOUR_UTC" in
  ''|*[!0-9]*) echo "BACKUP_HOUR_UTC must be an integer from 0 to 23" >&2; exit 2 ;;
esac
if [ "$BACKUP_HOUR_UTC" -gt 23 ]; then
  echo "BACKUP_HOUR_UTC must be an integer from 0 to 23" >&2
  exit 2
fi
while [ "${BACKUP_HOUR_UTC#0}" != "$BACKUP_HOUR_UTC" ]; do
  BACKUP_HOUR_UTC="${BACKUP_HOUR_UTC#0}"
done
: "${BACKUP_HOUR_UTC:=0}"

backup_once() {
  timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
  temporary="/backups/.open-community-${timestamp}.dump.tmp"
  destination="/backups/open-community-${timestamp}.dump"

  if ! pg_dump \
    --host="$PGHOST" \
    --port="$PGPORT" \
    --username="$POSTGRES_USER" \
    --dbname="$POSTGRES_DB" \
    --no-password \
    --format=custom \
    --file="$temporary"; then
    rm -f "$temporary"
    return 1
  fi
  chmod 0640 "$temporary"
  mv "$temporary" "$destination"

  find /backups -maxdepth 1 -type f -name 'open-community-*.dump' -mmin +20160 -delete
  echo "Created PostgreSQL backup: $destination"
}

seconds_until_next_backup() {
  current_hour="$(date -u +%H)"
  current_minute="$(date -u +%M)"
  current_hour="${current_hour#0}"
  current_minute="${current_minute#0}"
  : "${current_hour:=0}"
  : "${current_minute:=0}"
  hours_until=$(((BACKUP_HOUR_UTC - current_hour + 24) % 24))
  seconds_until=$((hours_until * 3600 - current_minute * 60))
  if [ "$seconds_until" -le 0 ]; then
    seconds_until=$((seconds_until + 86400))
  fi
  printf '%s\n' "$seconds_until"
}

while :; do
  backup_once
  sleep_seconds="$(seconds_until_next_backup)"
  echo "Next PostgreSQL backup in ${sleep_seconds}s (target ${BACKUP_HOUR_UTC}:00 UTC)"
  sleep "$sleep_seconds"
done
