# Round 72 evidence

Deliverable: two more specs converted, and a guard that stops the suite from ever again passing
against the wrong application.

## The batch: two of seven

Seven community specs got the round 69 recipe - `mockAppBootstrap` before their own routes so their
mocks still win, plus `mockHelpCenter`. Two went green (`community-resources`,
`community-integrations`) and five did not. The five were reverted rather than left half-converted:

| Spec | Result |
| --- | --- |
| community-resources | green |
| community-integrations | green |
| community-projects | reverted |
| community-proposals | reverted |
| community-governance | reverted |
| community-open-data | reverted |
| community-official-announcements | reverted |

## The finding: one of those five failures was not a missing route

`community-proposals` failed, and its captured error context showed:

```
- link "Universidad Pedagogica y Tecnologica de Colombia, inicio"
- paragraph "ESPACIO DE TRABAJO"
```

A Spanish university. Not this project.

The civic preview server had died partway through the batch, and the other Vite app on this machine
claimed port 3002. The suite then ran to completion against a landing page it was never meant to
load, failing assertions one by one, each pointing at a view that did not exist. `3002` serving
"Open Civic Signal OS" again by the end of the round is what made it intermittent rather than
consistent.

This is the same class of failure as round 63, where a capture script wrote another project's UI
into this repo's evidence. There it was caught because the script refused to write when a selector
was missing. Nothing caught it here: a global setup that checks who is on the port.

## The guard

`playwright.global-setup.ts` fetches the base URL once before any test and compares the `<title>` to
`Open Civic Signal OS`. A string comparison rather than a selector, so it does not depend on any
markup of the app under test, and one clear failure instead of forty misleading ones.

Both directions were verified rather than assumed:

```
BASE_URL=http://localhost:3002   ->  7 passed
BASE_URL=http://localhost:5199   ->  Error: http://localhost:5199 is serving
                                      "Plataforma Universitaria". Another project on this
                                      machine is using the port. ...
```

The message names the offending title, because "the wrong app is on the port" is much easier to act
on than "7 tests failed".

It lives in its own file rather than inline in `playwright.config.ts`: the inline function form
tripped Playwright's config typing, and a separate module is also the canonical shape for a setup
hook that does real work.

## Verified

`gate-run-ci-mode.txt` is the gate exactly as the workflow runs it, `CI=true`, against the production
build on port 3002: **80 passed** across both projects, no failures and no flakes. The gate now
covers 13 of 47 spec files.

The comparison across rounds: 76 passed at round 70 and 71, 80 now. Two specs converted and one
guard added, with no regression in anything already covered.

## Known limitations

- The five reverted specs need more than a missing route. Each was tried and rolled back rather than
  left in a state where it looks converted but is not.
- The preview server still dies on its own during a session. The guard turns that from a silent
  wrong-app run into an immediate, named failure, but it does not stop the crash.
- `community-threads-paging` was left alone: it uses `activeCommunityId` rather than a
  `communityId` constant and has no plain `page.route(` call to anchor the insertion to, so it needs
  a different recipe rather than the same one.
- No production code was touched, so the evidence is real test output rather than screenshots.