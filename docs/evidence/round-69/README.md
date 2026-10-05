# Round 69 evidence

Deliverable: seventeen specs that had never run anywhere now run. `field-data-mode` (4),
`weekly-digest` (7) and `merge-review` (6).

## The cause, three times over

Round 67 named it and rounds 68 and 69 kept confirming it: a spec that mocks the routes it knows
about is one unmocked request away from being useless, and the symptom points at the view.

- `field-data-mode` mocked `auth/me` and the dashboard panels but not `communities/my` or
  `help-center`.
- `weekly-digest` and `merge-review` mocked their three domain routes each and nothing else.

## Three fixtures were each missing something different

**A community-scoped view needs an active community.** `weekly-digest` and `merge-review` seed
`community-storage` in localStorage, and that looks sufficient - until `Layout` reloads memberships
from `communities/my`, gets the empty list this helper returned, and clears the selection. The view
then renders its empty prompt. Both specs now pass their community id to `mockAppBootstrap`, which
returns one membership; `setMemberships` keeps a still-valid selection and otherwise picks the first
membership, so no second endpoint is involved.

**Field mode needed the settings store.** `field-data-mode` has its own `seedSession` because it
writes `dataMode` as well as the session. Rather than keep a second copy of the auth seed, `dataMode`
is now an option on the shared helper, and the local function is gone.

**Settings has four routes of its own.** The test that reads the settings screen landed on `/login`
intermittently. `Settings` requests `auth/profile/me`, `auth/privacy/access-logs`,
`communities/{id}/privacy` and `signals/export/csv`; none were mocked. It passed or failed depending
on which won the race against the logout, which is the worst kind of failure to hand to the next
person.

## I nearly reported a green run that was not green

The ten-spec parallel run printed `35 passed` in its last line. I read that as success and started
writing it up. The saved output contained `2 failed` - the two test names listed above that line were
the failures, not the last tests to pass.

Looking in the artifact instead of at the summary tail is the only reason this was caught. Every
evidence file from this round is checked with a search for `failed` rather than by reading the last
line, and that check is now part of how this repo's rounds are verified.

## The suite is timing-fragile under parallel load

Isolated, and in small groups, all 37 tests pass. Under parallel load against a single Vite dev
server, timeouts appear and grow with the number of spec files in the run:

| Run | Result |
| --- | --- |
| 2 specs | 11 passed |
| 7 specs, 4 workers | 4 failed, 22 passed |
| 10 specs, 4 workers | 2 failed, 35 passed |
| **10 specs, 1 worker** | **37 passed** |

So the flakiness is contention, not a defect in the tests, and it is a property of the suite rather
than of this round: several specs assert on outgoing requests with fixed `waitForTimeout` sleeps, and
one Vite dev server serving four browsers does not always answer inside 30s.

`playwright-run-serial.txt` is the run this round claims. `full-run-10-specs.txt` is the parallel run,
kept rather than deleted because the difference between them is the finding.

This is worth fixing properly and is queued: the sleeps are tuned guesses, and the repo's own
guidance elsewhere says a settled condition is better than a tuned wait. It also needs a decision
about how the suite is run in CI, given nothing runs it today.

## Reverted

`community-rooms-history` was converted the same way and reverted again. The logout is fixed by the
shared bootstrap, but the rooms view still renders nothing, so it has its own problem beyond missing
routes. Two attempts are enough to say it is not this pattern.

`dashboard-guided-home` is untouched: it needs seeded memberships *and* a role switch through the
settings screen, which is its own piece of work.

## Known limitations

- Seventeen tests revived out of the set that had never run. `dashboard-guided-home` and
  `community-rooms-history` remain, plus whatever else was in the original count of 26, which is still
  not a meaningful number.
- No production code was touched, so the evidence is real test output rather than screenshots.
- The Vite dev server on 5299 was restarted again during this round. It is launched detached and
  something reaps it; it has now died five times this session.