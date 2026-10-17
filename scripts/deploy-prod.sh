#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091 # The shared file is resolved relative to this script at runtime.
source "$SCRIPT_DIR/prod-compose-common.sh"

prod_require_files
prod_compose config --quiet
bash "$SCRIPT_DIR/backup-prod.sh"
git -C "$PROD_ROOT_DIR" pull --ff-only
prod_compose up --detach --build
prod_wait_for_stack
