# Round 77 evidence

Deliverable: `community-projects` is green and has joined the gate. The spec's board-creation,
task-create, task-move and task-note flow now runs end to end against the production build.

## What was wrong, and why the test had to change

The spec had a contradiction that made it unpassable by any means short of rewriting the test.

It seeded a task on the original board, asserted it in the todo column before creating a board, then
created a second board. Creating a board selects it, so the seeded board's task leaves the screen.
After that the spec asserted the same task had moved to the In progress column, and the task-move
click targeted that task's id. The view was showing a different board, so that button could never
render.

The PATCH mock shared the same confusion: it rewrote `boards[0]`, which after creation is the *new*
board, so it patched a task that board does not contain.

Two fixes, both about making the spec say what its own name says - "creates a board, adds a task,
moves it, and leaves a task note":

1. `createdTaskId` is now a named id, and the mocks, the move click, the note input and the final
   assertion all use it. The test moves the task it created.
2. The In progress assertion now expects "Schedule Saturday volunteer shift" rather than "Confirm
   school committee palette". The seeded task is still asserted in the todo column at line 277,
   before the board exists, where that assertion is meaningful.

Nothing in the production code changed. Both changes make the spec match the behaviour the test was
written to check, and neither weakens an assertion into a tautology: the move is still verified
through the PATCH payload (`status: "IN_PROGRESS"`) as well as the column.

## Why the In progress assertion was changed rather than made true

The obvious alternative was to keep the seeded task and teach the mock to move it. That would have
required the detail card to keep showing the original board after creating a new one, which is not
how the product behaves and not what the test name describes. Editing the assertion to name the task
that was actually moved is the smaller change and the honest one.

This is worth flagging rather than burying: it is the second time this spec asserted something its
name contradicted. Round 76 found the membership role was silently downgraded for all 16 converted
specs; this round found the assertion target was stale. Both were written to look plausible, and
neither failed loudly - they failed by timing out, which is the slowest possible signal.

## Verification

`projects-after-task-id.txt` - the spec alone, 1 passed (4.9s).

`gate-run-ci-mode.txt` - the full gate after the change, **88 passed**, no failures, no flaky, 17
specs. Round 75 was 86 passed across 16 specs, so this is exactly the two tests one new spec should
add, with nothing else moved.

`timestamps` and TypeScript were not involved: no production file was touched, so the only build
input that changed is the test file, and `tsc --noEmit` passed before the runs.

## Known limitations

- `community-proposals` still waits for a moderation control that never renders.
- `community-open-data` and `community-official-announcements` still need a product decision: whether
  token scopes should have per-scope test hooks, and which screen owns pinned announcements.
- The other specs converted in rounds 73 to 75 remain unexamined for the same class of stale
  assertion found here. That is the next place to look, and it needs a way to tell "asserted the
  wrong thing" from "asserted nothing", which is why this round's evidence names the trap.
- Evidence is test output, not screenshots: no UI was modified.