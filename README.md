<p align="center">
  <img src="assets/logo.svg" alt="Open Civic Signal OS" width="760" />
</p>

# Open Civic Signal OS

Monorepo civic-tech platform that converts community signals into transparent public priorities.

## Official Stack

- Backend: Java 21 + Spring Boot
- Frontend: React + TypeScript + Vite
- Data: PostgreSQL (planned), JSON-first MVP now
- Contracts: OpenAPI in `packages/contracts`

## Monorepo Layout

- `apps/api-java`: Spring Boot API and prioritization services
- `apps/web-react`: public dashboard and operator console
- `packages/contracts`: API contracts and shared schemas
- `infra`: local environment and deployment assets
- `docs`: strategy, ideas, architecture, and execution plans

## Quick start

### Current MVP scripts

```bash
npm install
npm run prioritize
```

### Web app

```bash
cd apps/web-react
npm install
npm run dev
```

### Java API

```bash
cd apps/api-java
./mvnw spring-boot:run
```

## Launch resources

- Demo GIF: `assets/demo.gif`
- Architecture GIF: `assets/architecture.gif`\n- Architecture SVG: `assets/stack-map.svg`
- Landing: `docs/index.html`
- Execution ideas: `docs/ideas.md`
- Agent playbook: `AGENTS.md`

## Roadmap issues

- https://github.com/jdsalasca/open-civic-signal-os/issues

## Vision

Community voice should become visible, measurable, and actionable.

## Visual Assets

- Brand kit: `assets/brand-kit.md`
- Mark: `assets/logo-mark.svg`
- Wordmark: `assets/logo-wordmark.svg`
- Banner SVG: `assets/banner.svg`
- Social card PNG: `assets/social-card.png`
- Logo PNG: `assets/logo-512.png`

## Docker Quickstart

- Dev (hot reload): `npm run docker:dev:up`
- Prod-like: `npm run docker:prod:up`
- Stop: `npm run docker:dev:down`

See full guide: `docs/DOCKER_CLAUDE.md` and `CLAUDE.md`.
