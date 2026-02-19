# Playwright Visual Protocol (Mandatory)

Visual UX quality represents 70% of perceived project value for this product.  
Any frontend-impacting PR must include Playwright CLI visual evidence.

## Prerequisite (required)

Verify `npx` is available:

```bash
command -v npx >/dev/null 2>&1
```

If missing, run:

```bash
# Verify Node/npm are installed
node --version
npm --version

# If missing, install Node.js/npm, then:
npm install -g @playwright/cli@latest
playwright-cli --help
```

## Canonical Command

Use repository wrapper:

```bash
npm run agent:ux:pw -- <playwright-cli-command>
```

The wrapper prefers Codex skill wrapper (`$CODEX_HOME/skills/playwright/scripts/playwright_cli.sh`) and falls back to `npx @playwright/cli`.

## Required Artifact Location

Save screenshots/snapshots under:

- `output/playwright/`

Do not commit generated visual artifacts unless explicitly requested.

## Required Civic Frontend Flow Audit

For PRs touching frontend files, capture at minimum:

1. Dashboard load (`/`)
2. Report flow (`/report`)
3. Signal detail flow (`/signal/:id`)
4. Register flow (`/register`) when auth/onboarding is touched

Example flow:

```bash
npm run dev:web
npm run agent:ux:pw -- open http://localhost:5173 --headed
npm run agent:ux:pw -- snapshot
npm run agent:ux:pw -- screenshot output/playwright/dashboard.png
```

## PR Requirement

PR must include section `## UX Evidence (Playwright)` with:

- commands executed
- pages audited
- artifact paths (or explicit reason for `N/A`)
