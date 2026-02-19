# Monorepo Bootstrap

## First-time setup

1. Install Node 20+ and Java 21.
2. From repo root run:

`ash
npm install
npm run prioritize
`

3. Start backend:

`ash
cd apps/api-java
mvn spring-boot:run
`

4. Start frontend:

`ash
cd apps/web-react
npm install
npm run dev
`

## Working agreements for agents

- Do not merge logic-only changes without docs updates.
- Keep OpenAPI updated before frontend integration.
- Attach sample payloads to every feature PR.
- Use milestone + P0/P1/P2 labels in all planning issues.
