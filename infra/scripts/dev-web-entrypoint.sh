#!/usr/bin/env sh
set -eu

cd /workspace

echo "[web-dev] Installing dependencies with retry..."
attempt=1
while [ "$attempt" -le 20 ]; do
  if npm ci --no-audit --no-fund; then
    echo "[web-dev] Dependencies ready."
    break
  fi
  echo "[web-dev] npm ci failed (attempt ${attempt}/20). Retrying in 5s..."
  attempt=$((attempt + 1))
  sleep 5
done

echo "[web-dev] Starting Vite dev server with polling hot reload..."
exec npm --workspace apps/web-react run dev -- --host 0.0.0.0 --port 5173
