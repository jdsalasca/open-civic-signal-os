#!/usr/bin/env bash
set -Eeuo pipefail

PROD_SCRIPTS_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROD_ROOT_DIR="${PROD_ROOT_DIR:-$(cd -- "$PROD_SCRIPTS_DIR/.." && pwd)}"
PROD_COMPOSE_FILE="${PROD_COMPOSE_FILE:-$PROD_ROOT_DIR/infra/docker-compose.prod.yml}"
PROD_ENV_FILE="${PROD_ENV_FILE:-$PROD_ROOT_DIR/infra/.env.prod}"
COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-open-community}"
PROD_BACKUP_DIR="${PROD_BACKUP_DIR:-$PROD_ROOT_DIR/backups}"

prod_require_files() {
  [[ -f "$PROD_COMPOSE_FILE" ]] || { echo "Production Compose file not found: $PROD_COMPOSE_FILE" >&2; return 1; }
  [[ -f "$PROD_ENV_FILE" ]] || { echo "Production environment file not found: $PROD_ENV_FILE (copy infra/.env.prod.example)" >&2; return 1; }
  command -v docker >/dev/null 2>&1 || { echo "Docker is required to manage the production stack." >&2; return 1; }
}

prod_compose() {
  docker compose \
    --project-name "$COMPOSE_PROJECT_NAME" \
    --file "$PROD_COMPOSE_FILE" \
    --env-file "$PROD_ENV_FILE" \
    "$@"
}

prod_wait_for_health() {
  local service="$1"
  local timeout_seconds="${PROD_HEALTH_TIMEOUT_SECONDS:-180}"
  local poll_seconds="${PROD_HEALTH_POLL_SECONDS:-5}"
  local deadline=$((SECONDS + timeout_seconds))
  local container_id status

  while (( SECONDS <= deadline )); do
    container_id="$(prod_compose ps --all --quiet "$service" 2>/dev/null || true)"
    if [[ -n "$container_id" ]]; then
      status="$(docker inspect --format '{{.State.Health.Status}}' "$container_id" 2>/dev/null || true)"
      case "$status" in
        healthy) return 0 ;;
        unhealthy)
          echo "Service '$service' reported unhealthy." >&2
          prod_compose logs --no-color --tail=100 "$service" >&2 || true
          return 1
          ;;
      esac
    else
      status="missing"
    fi
    sleep "$poll_seconds"
  done

  echo "Service '$service' did not become healthy within ${timeout_seconds}s (last status: ${status:-unknown})." >&2
  prod_compose logs --no-color --tail=100 "$service" >&2 || true
  return 1
}

prod_wait_for_stack() {
  local service
  for service in db api web; do
    prod_wait_for_health "$service" || return 1
  done
  echo "Production stack is healthy: db, api, and web are healthy."
}
