# Round 43 evidence

Deliverable: the weekly digest **preview** a coordinator reads is the same artifact publishing sends.

## The defect

Round 41 made the prepared digest the artifact publishing uses. It did not touch the controller, so
`GET /api/community/weekly-digest` still called `buildDigest`, which recomposes from live data while
`POST /publish` used the stored preparation.

The service-level tests all passed, because they called the service directly. The controller is where
the preview actually lived.

## Why the first version of the test did not catch it

The test took the preview **before** mutating the world. A preview taken before the change recomposes
to the same bytes as the artifact, so the defect is invisible. The order matters:

```
prepare → rescore → preview → publish
```

Rescoring first is what makes the recomposition observable.

## The test I first wrote was wrong, not the code

The third assertion compared `indexOf("1. Water main break")` with `indexOf("1. Streetlight out")`. In
the sealed artifact Streetlight is item **2**, so `indexOf("1. Streetlight out")` returned `-1` and the
comparison always failed. It took a `System.out` probe to see that the preparation existed, held the
right body, and the assertion was still wrong.

Replaced with assertions on the actual rendered ranks, plus a `storedPreparation.isPresent()` check so
a preparation that silently failed to happen cannot make the rest of the test agree with itself.

## Verified by mutation

The fix is a one-line change in `WeeklyDigestController`. Reverting it to
`buildDigestForScheduler` was run to confirm the test actually catches the regression:

```
[ERROR] Tests run: 1, Failures: 1
[ERROR] the digest a coordinator reviewed must be the digest residents receive
```

With the fix restored:

```
[INFO] Tests run: 10, Failures: 0, Errors: 0 -- in org.opencivic.signalos.WeeklyDigestIT
```

A test that passes with and without the fix is not testing the fix.

## Also changed

`prepareForAllCommunities(UUID, String weekKey)` overload. "The week that just ended" is only right
for a weekly cron; a community closing its books late, or an operator preparing a week by hand, needs
to name the week. Without it the test could only prepare whatever the calendar currently said.

`WeeklyDigestService.buildDigest` deleted. After the controller moved to `digestForPreview` it had no
production callers, and two names for one behaviour is how the wrong one gets wired up next time.

## Full verification

`mvn clean test` — see `mvn-clean-test.txt`:

```
[INFO] Tests run: 413, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`npm run agent:preflight` — see `agent-preflight.txt`:

```
OpenAPI parity check passed: 178 routes across 194 controller sources,
  147 documented paths, 9 intentionally undocumented.
Agent preflight passed.
```

Clean build rather than incremental: twice this session a stale `target/` produced a convincing
failure that had nothing to do with the change — a deleted `V34` migration still in `target/classes`,
and an `application-test.yml` in `target/test-classes` missing the credential key.