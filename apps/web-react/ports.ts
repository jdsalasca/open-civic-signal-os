/**
 * The port the web app is served on, in one place.
 *
 * Three files needed this number and disagreed: the dev script passed `--port 3002`, the Vite config
 * said `server.port: 5173`, and the Playwright config assumed `http://localhost:3002`. Vite takes the
 * last `--port`, so the config value was silently dead and a developer reading the config got a
 * different answer from the one the app actually used.
 *
 * A shared constant removes the class of bug rather than testing for it: there is no second place
 * to drift. The Docker files keep their own literals because Compose cannot import TypeScript, and
 * those are a different layer.
 *
 * The dev container overrides this on purpose - `infra/scripts/dev-web-entrypoint.sh` passes
 * `--port 5173` to match its published mapping - which a command-line flag still wins over.
 */
export const WEB_PORT = 3002;