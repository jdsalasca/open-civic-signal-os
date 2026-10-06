# Round 71 evidence

Deliverable: one port, declared once. And the reason the previous round's config edits were not
taking effect at all.

## What the disagreement was

Three files carried the web port and none of them agreed:

```
playwright.config.ts   baseURL -> http://localhost:3002
package.json           "dev": "vite --port 3002"
vite.config.ts         server.port: 5173
```

Vite takes the last `--port`, so the config value was dead and a developer reading `vite.config.ts`
got a different answer from the one the app actually used.

`3002` is the canonical one: the production compose publishes it, the GHCR compose publishes it, the
CORS default points at it, and the Playwright baseURL assumed it. `5173` only appeared in the dev
compose, whose container passes `--port 5173` explicitly - so moving the config to 3002 cannot break
the `5173:5173` mapping, a command-line flag still wins.

## The edit silently did nothing, and that is the real finding

Changing `vite.config.ts` to `WEB_PORT` produced no behaviour change at all. `vite preview` still
bound 4173.

`apps/web-react/vite.config.js` existed, was **committed to git**, and contained the stale
`server.port: 5173`. `tsconfig.node.json` is a composite project with no `outDir`, so `tsc -b` emitted
the compiled config next to the source, and Vite resolves `vite.config.js` before `vite.config.ts`.

So every past edit to `vite.config.ts` in this repo was a no-op. This was caught only because the
round verified the actual listening port instead of trusting that the edit looked right - the config
diff was correct and the behaviour was not.

Three generated files were tracked: `vite.config.js`, `vite.config.d.ts` and
`tsconfig.node.tsbuildinfo`. AGENTS.md forbids committing generated artifacts, and in this case one
of them was shadowing the source it was generated from.

The fix is structural rather than a test: `ports.ts` exports `WEB_PORT`, and both `vite.config.ts`
and `playwright.config.ts` import it. There is no second place left to drift. The emitted files are
deleted, redirected to `node_modules/.tmp` via `outDir`, and gitignored.

A test asserting the two configs agree would have passed while the behaviour was still broken,
because both would have agreed on a value the shadowing `.js` overrode. The shadowing is the defect,
so the shadowing is what had to go.

## Verified by behaviour, not by reading the diff

```
npm run dev          -> http://localhost:3002   (no --port flag)
npx vite preview     -> http://localhost:3002   (no --port flag)
npm run build        -> does not recreate vite.config.js / .d.ts / .tsbuildinfo
git status           -> the three artifacts stay gone
```

The dev server was also observed moving to 3003 when 3002 was already taken, which is the behaviour
of a config with no hardcoded port and is the clearest available evidence that the flag is gone.

The gate then ran exactly as the workflow runs it, `CI=true`, against the production build:
**76 passed**, no failures and no flakes. Evidence in `gate-after-port-change.txt`.

## Workflow consequence

`playwright.yml` dropped its explicit `--port 3002` from the preview step and the explicit curl URL
became a quoted literal. The port comes from the config now, so the workflow cannot drift away from
what a local `npm run preview` serves.

## Known limitations

- The Docker files still carry port literals, because Compose cannot import TypeScript. They are a
  different layer and they all agree on 3002 for the host-facing port.
- `docker-compose.dev.yml` still publishes `5173:5173`, which is correct for its container and
  unrelated to the host default.
- The gate still covers 11 of 47 spec files. That is round 69's work continuing, not this round's.
- No production UI code was touched, so the evidence is test output rather than screenshots.