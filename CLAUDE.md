# CLAUDE.md

## Objective

Run, test, and deploy Open Civic Signal OS reliably with Docker.

## Canonical Commands

### Dev mode

```bash
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up --build
```

### Prod-like mode

```bash
docker compose -f infra/docker-compose.yml up --build -d
```

### Stop

```bash
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml down
```

## Verification Checklist

- API: `http://localhost:8081/actuator/health`
- Web: `http://localhost:3002` (prod-like) or `http://localhost:5173` (dev)
- If web cannot reach API, check `VITE_API_BASE_URL` and nginx `/api` proxy.

## Constraints

- Do not move ranking logic to frontend.
- Do not deploy with failing health checks.
- Keep compose files as single source of runtime truth.
