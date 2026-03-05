#!/usr/bin/env bash
set -euo pipefail

cd /workspace/apps/api-java

echo "[api-dev] Resolving Maven dependencies with retry..."
for attempt in $(seq 1 20); do
  if mvn -B -q -DskipTests dependency:go-offline; then
    echo "[api-dev] Maven dependencies ready."
    break
  fi
  echo "[api-dev] dependency:go-offline failed (attempt ${attempt}/20). Retrying in 5s..."
  sleep 5
done

echo "[api-dev] Starting Spring Boot with devtools hot reload..."
exec mvn -B -DskipTests \
  -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.fork=false \
  -Dspring-boot.run.addResources=true \
  spring-boot:run
