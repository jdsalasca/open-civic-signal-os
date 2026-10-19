# Round 46 evidence

Deliverable: the transparency report states in its payload what it cannot reproduce.

## Two false claims found

Round 45 fixed the audit-trail defect and wrote down the remaining gap: priority scores have no history.
Reading the code for this round turned up two places where the platform was promising more than it
delivers.

**The record's javadoc:**

> Deterministic by construction: everything is derived from the stored period bounds, so regenerating
> the same month later yields the same figures. Nothing here reads "now" except `generatedAt`.

False for the scores. A consumer told a report is reproducible will reasonably rely on that, and then
use it.

**The OpenAPI description of `generatedAt`:**

> The only field that varies between two runs over the same period.

Also false for the same reason. So the contract itself was wrong, not only the code comment — which
means the gap was invisible to anyone integrating from the spec.

## The fix

`reproducibilityLimits` travels **in the payload**, not only in documentation. The person who needs it is
whoever consumes the JSON — a dashboard, a municipality, a journalist — and they will never read this
service's javadoc. It is currently always non-empty, and it says precisely what holds and what does not:
counts, statuses and list positions come from the audit trail and are reproducible; the score does not.

Both false descriptions were corrected rather than left standing next to the new field.

## Verified by mutation

The test was written first and failed with `No value at JSON path "$.reproducibilityLimits"`, then
passed once the field existed. `theReportMustSayThatScoresAreNotReproducible` asserts the list is
non-empty **and** that the first entry names the score specifically, so it cannot be satisfied by a
generic disclaimer that mentions nothing.

## Why the gap is disclosed rather than fixed here

A score ledger means deciding when a score becomes official — at report time, at rescore time, on
approval. That is a governance decision, and making it while chasing a reproducibility bug is how you
get a ledger nobody trusts. The cheap half is telling the truth about the current state, which is what
this round does; the ADR says so rather than implying the work is finished.

## Full verification

`mvn clean test` — `mvn-clean-test.txt`:

```
[INFO] Tests run: 424, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`npm run agent:preflight` — `agent-preflight.txt`: `Agent preflight passed.`, `Contract change with ADR
update detected; gate passed.`

`npm run contract:parity:check` — `contract-parity.txt`: `178 routes across 194 controller sources,
147 documented paths, 9 intentionally undocumented.`