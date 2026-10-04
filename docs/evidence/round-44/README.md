# Round 44 evidence

Deliverable: one definition of "this issue is settled", shared by every ranking in the platform.

## The defect

There were **eight** private definitions of whether a signal is still open:

- Seven services each carried `private static final Set<String> CLOSED_STATUSES = Set.of("RESOLVED", "CLOSED", "REJECTED")`
- Round 42's federated backlog added an eighth, written against the `SignalStatus` enum

`CLOSED` predates the enum and is not a member of it. So the eighth disagreed with the other seven: a
signal whose status was the legacy `"CLOSED"` was excluded from every ranking in the platform and
**included** in the federated backlog.

Nothing crashed. One public view showed a settled issue as pending while six others showed it
resolved. That is the worst way for this to break: every individual view is defensible and the platform
still answers two different questions.

The divergence was introduced by me in round 42, in the same commit where I wrote a comment explaining
why I was avoiding a duplicated list — and then duplicated the list anyway, with different contents.

## The fix

`SignalStatus.isSettled()` and `SignalStatus.isSettled(String)` live on the enum, next to the states they
describe, and the eight private constants are gone. `LEGACY_SETTLED` keeps `"CLOSED"` honoured with a
comment saying why it is not decoration: a ranking that dropped it would resurrect a settled issue in
whichever view changed.

`PrioritizationFormula` already exists because the weights were once copied three times and a test
compared them. Two copies compared by a test are still two copies. This is the same failure with a
filter instead of arithmetic.

## Verified by mutation

Emptying `LEGACY_SETTLED` — the smallest possible change that reintroduces the divergence:

```
[ERROR] Tests run: 9, Failures: 1 -- FederationCityBacklogIT
[ERROR]   aLegacyClosedIssueLeavesEveryRankingIncludingTheFederatedOne
[ERROR] Tests run: 5, Failures: 1 -- SignalStatusResolutionTest
[ERROR]   theLegacyClosedStringIsSettled
```

Both bite. `aLegacyClosedIssueLeavesEveryRankingIncludingTheFederatedOne` gives the legacy status the
**highest score in the city**, so the test cannot pass by accident on ordering.

`anUnknownStatusCountsAsOpenSoAReportIsNeverSilentlyDropped` pins the other direction: a status this
version has not heard of is not evidence that anyone settled the issue, so the report stays visible.

## Full verification

`mvn clean test` — `mvn-clean-test.txt`:

```
[INFO] Tests run: 420, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`npm run agent:preflight` — `agent-preflight.txt`: `Agent preflight passed.`

`npm run contract:parity:check` — `contract-parity.txt`:

```
178 routes across 194 controller sources, 147 documented paths, 9 intentionally undocumented.
```

Seven services changed, all of which had a passing test suite asserting their old behaviour. Clean
build rather than incremental, because twice this session a stale `target/` produced a convincing
failure unrelated to the change.