# Round 73 evidence

Deliverable: the reason six specs ran against a completely different application, found and fixed.
They still fail, but they now fail for their own reasons instead of someone else's.

## The finding

Every one of these specs hardcoded an absolute URL:

```ts
await page.goto("http://127.0.0.1:5173/communities/governance");
```

Port 5173 on this machine is another project's dev server. The specs ignored `baseURL` entirely, so
`BASE_URL` had no effect on them at all, and the captured error context for `community-governance`
showed a Spanish university navigation while the suite was pointed at the civic app on 3123.

That also explains why the round 72 guard "did not fire". It was working correctly: it checked the
URL it was configured with and found civic. The spec then navigated somewhere else entirely.

Six specs had this:

```
community-decisions      community-governance    community-open-data
community-projects        community-proposals     community-trust-metrics
```

All six now navigate relatively, so `baseURL` applies and one server choice governs the whole suite.
There are no absolute `goto` URLs left in the directory.

## Verified by what the page actually rendered

Before the fix, the failure context for `community-governance` contained:

```
Universidad Pedagogica y Tecnologica de Colombia
ESPACIO DE TRABAJO
```

After it, the same context contains:

```
status: Governance document created.
link "Skip to main content"  ->  /url: "#main-content"
```

That is the civic app. The evidence for this round is the change in what the page rendered, not the
pass count - because the pass count is still zero for these specs.

## The guard now runs per test, and that is a different guarantee

Round 72 added a `globalSetup` check, which runs once. It cannot catch a port that changes while the
suite is already running, so `assertCivicAppIsServed` now also runs from `mockAppBootstrap`, which
every converted spec calls before it navigates. Same message, but it fires per test.

Its limit is now documented honestly in the code: it verifies the URL a test is about to use, not the
URL a hardcoded `goto` sends the browser to. That gap is precisely what this round's defect exploited,
and the fix for the defect was to remove the hardcoded URLs, not to widen the guard.

## What is still failing, and why it is not mechanical

With the right app rendering, the remaining failures are content assertions that a mocked create
does not put into the view's state:

```
Expected substring: "Approve the safer crossing rollout"
Expected substring: "Water interruption notice"
Expected substring: "Los Rosales"
```

A spec that creates a board through a mocked `POST` and then expects the created name on screen needs
the mock's response to be what the view stores, or needs the view to add it optimistically. That is a
per-view question, not a route list, which is why three attempts at the mechanical recipe did not
work and the fourth - reading where the browser actually went - did.

`community-official-announcements` still fails too, and it never had a hardcoded URL.

## Gate regression check

The shared helper changed, so the gate was re-run before committing: **78 passed, 0 failed, 2 flaky**
in `CI=true` mode against the production build. The two flaky are:

- `community-integrations` on chromium
- `community-resources` on mobile-chrome

Both passed on retry, which is what `retries: 2` exists for, but they are timing-sensitive and should
not be mistaken for stable. The gate is green in CI mode with `workers: 1`; local runs used port 3123
because 3002 is contested by another project on this machine.

## Known limitations

- Six specs are fixed to respect `baseURL` and still fail on their own content assertions. That work
  is per-view and is the next round.
- The two flaky tests above are load-sensitive and were not fixed here.
- The local preview server still dies on its own during long sessions. Using a dedicated port makes
  that a loud connection failure instead of a silent wrong-app run, which is the improvement
  available from the tooling side.
- No production code was touched, so the evidence is test output rather than screenshots.