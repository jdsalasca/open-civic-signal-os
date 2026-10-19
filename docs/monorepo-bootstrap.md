# Monorepo Bootstrap

## First-time setup

1. Install Node 20+ and Java 21.
2. From repo root run:

```bash
npm install
npm run prioritize
```

3. Start the integrated hot-reload runtime:

```bash
npm run docker:dev:up
```

4. Check runtime health when needed:

```bash
npm run docker:dev:doctor
npm run docker:dev:ps
npm run docker:dev:logs
```

## Working agreements for agents

- Do not merge logic-only changes without docs updates.
- Keep OpenAPI updated before frontend integration.
- Attach sample payloads to every feature PR.
- Use milestone + P0/P1/P2 labels in all planning issues.
- Use Docker as the canonical integrated runtime; do not silently replace it with bare frontend/backend local processes during agent validation.
