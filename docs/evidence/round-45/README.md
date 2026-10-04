# Round 45 evidence

Deliverable: a closed month's transparency report answers from the audit trail, not from live state.

## The defect

`unaddressedSignals` filtered on `signal.status`, the mutable column. Its two siblings in the same
report already did the right thing: `SIGNALS_STILL_OPEN` counted from the audit trail bounded by the
period end, and `actionedSignals` read the status entries.

So the report disagreed with itself. February published "1 still open" in its metrics next to an
unaddressed list that omitted the same issue.

A closed month's record of what a community failed to address **shrank every time the community did the
work later**. A fence fixed in April silently vanished from February's list, with no trace that the
published figure had changed. Nothing errored; every individual number was defensible on its own.

This is the third instance of one shape in three rounds: a read path answering from live state what a
sibling answered from history — `CLOSED_STATUSES` in round 44, the digest preview in round 43, and the
unaddressed list here.

## The fix

Reported in the period, and no settled status entry at or before the period end — the same rule the
metric beside it already used. `statusAt` shows the status as it stood then rather than today's.

Safe because settled states are terminal: `SignalStatus.canTransitionTo` refuses to leave `RESOLVED` or
`REJECTED`, so "was it settled by then" cannot change after the fact.

## Verified by mutation

Reverting the filter to the live status:

```
[ERROR] Tests run: 9, Failures: 2
[ERROR]   aClosedMonthShouldNotRewriteItselfWhenIssuesAreSettledAfterItEnded
[ERROR]   theStillOpenCountAndTheUnaddressedListMustAgree
```

The second is the strong one. `theStillOpenCountAndTheUnaddressedListMustAgree` asserts that the metric
and the list in one report answer the same question, so it fails on **internal inconsistency** rather
than on a fixture. It cannot be satisfied by getting one side right, which is precisely how this bug
would have survived a test written only against the expected number.

## What is still not reproducible, and is now written down

**Priority scores have no history.** `priorityScore` is mutable with no ledger, so the score shown for a
past period is today's score. Status and list membership are correct; the score is not, and nothing in
the response says so. The ADR records the gap rather than closing it — fixing it means deciding when a
score becomes official, and disclosing it in the response is the cheaper half that is still outstanding.

## Full verification

`mvn clean test` — `mvn-clean-test.txt`:

```
[INFO] Tests run: 423, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`npm run agent:preflight` — `agent-preflight.txt`: `Agent preflight passed.`, `Contract change with ADR
update detected; gate passed.`

`npm run contract:parity:check` — `contract-parity.txt`: `178 routes across 194 controller sources,
147 documented paths, 9 intentionally undocumented.`