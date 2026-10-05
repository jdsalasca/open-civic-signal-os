# Round 49 evidence

Deliverable: a closed assembly reports the time it actually ran.

## The defect

`elapsedMinutes` was measured against `LocalDateTime.now()` regardless of the assembly's state. An
assembly still open legitimately reads live — that is what a facilitation view is for. An assembly
**closed** kept counting for ever: read a month later, a forty-minute townhall reported 4000 minutes.

Same failure as the past transparency report recomputing its figures: a record that does not hold still.
This is the sixth instance of that shape, and it turned up in the one domain the previous five rounds
did not touch.

## My first hypothesis was wrong, and the tests said so

Three attempts, each of which I believed:

1. **Sleep 1.1 s and compare two reads.** Passed immediately. `elapsed` is measured in whole minutes, so
   1.1 seconds cannot cross a minute boundary — the test could not distinguish a frozen figure from a
   growing one. Not a bug in the code; a bug in the test.
2. **Backdate `openedAt` by three days, then close and read.** Failed with 4320 minutes even *with* the
   fix applied. A diagnostic assertion dumping `openedAt`, `closedAt` and the loaded class showed
   `closedAt` was set correctly three days after `openedAt` — so 4320 was the truthful answer for the
   data I had created. The test was wrong again, and I had been about to "fix" working code because of it.
3. **Backdate both timestamps by different amounts** — opened 40 days ago, closed 30 days ago. Now
   "when it closed" and "when this is read" are 10 days apart, and a correct reading is ~14400 minutes
   against ~57600 for a clock-based one. This one bites.

The lesson is the same one from rounds 43 and 48: **a test that passes on broken code is not evidence.**
Here it took three attempts and a diagnostic dump to notice the first two were asserting nothing.

## Verified by mutation

Reverting to `ChronoUnit.MINUTES.between(openedAt, LocalDateTime.now())`:

```
[ERROR] Tests run: 12, Failures: 1
[ERROR]   a closed assembly must report the span between openedAt and closedAt,
          not between openedAt and now; expected about 14400 minutes ...
```

## Also audited this round, with negative results

Continuing the read/write audit from the plan, two candidates came back clean and are recorded so they
are not re-audited blindly:

- **Participatory budget.** The money path reads `amountMinor` from the stored allocation rather than
  recomputing it. Proposal titles are read live, which is right — they link to the live proposal.
- **Backlog publication.** `orderingHash` sorts its lines before hashing, so it is a set hash as the
  javadoc claims. The deliberate decision not to hash display order is explained in the ADR and matches
  the implementation.

## Full verification

- `mvn clean test`: **430 pass, 0 fail** (was 429; +1) — `mvn-clean-test.txt`
- `npm run agent:preflight`: `Agent preflight passed.` — `agent-preflight.txt`
- `npm run contract:parity:check`: `178 routes across 194 controller sources, 147 documented paths,
  9 intentionally undocumented.` — `contract-parity.txt`
- Targeted: `AssemblyFacilitationIT` 12 pass, `CommunityAssemblyIT` 11 pass