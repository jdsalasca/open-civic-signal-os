# Docker + Docker Compose Baseline

This guide is the default for local development and fast deployment from Claude/Gemini/Codex agents.

## Services and Ports

- API: `http://localhost:8081`
- Web (prod-like): `http://localhost:3002`
- Web (dev/Vite): `http://localhost:5173`

## Local Development (hot reload)

```bash
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up --build
```

## Local Production-like Run

```bash
docker compose -f infra/docker-compose.yml up --build -d
```

## Stop and Clean

```bash
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml down
```

## Claude Deployment Flow

Use this sequence in Claude:

1. Build and start:
   - `docker compose -f infra/docker-compose.yml up --build -d`
2. Verify health:
   - API health endpoint and web root reachable.
3. Pull logs if needed:
   - `docker compose -f infra/docker-compose.yml logs --tail=200`
4. Update deployment:
   - `git pull` then rerun `up --build -d`.

## Rules

- Frontend consumes API via `VITE_API_BASE_URL`.
- Production image uses Nginx proxy for `/api`.
- Backend remains source of truth for prioritization logic.

## GHCR Image Deployment

Images are built in `.github/workflows/docker-images.yml`.

Default image names:

- `ghcr.io/jdsalasca/open-civic-signal-os-api:latest`
- `ghcr.io/jdsalasca/open-civic-signal-os-web:latest`

Run with GHCR images:

```bash
API_IMAGE=ghcr.io/jdsalasca/open-civic-signal-os-api:latest \
WEB_IMAGE=ghcr.io/jdsalasca/open-civic-signal-os-web:latest \
docker compose -f infra/docker-compose.yml -f infra/docker-compose.ghcr.yml up -d
```
