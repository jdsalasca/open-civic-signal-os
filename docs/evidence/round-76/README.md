# Round 76 evidence

Deliverable: a regression I introduced in rounds 73 to 75 found and fixed, and a test unblocked far
enough to show what the next real problem is.

## The bug was mine

`community-projects` failed on `project-board-submit-button` never becoming actionable:

```
locator.click: Test timeout of 30000ms exceeded
  52 × waiting for element to be visible, enabled and stable
```

The button carries `disabled={!canManageBoards}`, and `canManageBoards` is:

```ts
activeMembership?.role === "COORDINATOR" || activeMembership?.role === "PUBLIC_SERVANT_LIAISON"
```

The spec seeds `role: "COORDINATOR"` in `community-storage`. But the community store reloads
memberships from `communities/my` on every authenticated page, and `mockAppBootstrap` - the helper I
added in round 73 - returned a membership with `role: "MEMBER"`. `setMemberships` replaces the list,
so the spec's coordinator role was silently overwritten with MEMBER, the button stayed disabled, and
the click expired.

Every spec I converted with `mockAppBootstrap(page, communityId)` inherited that downgrade. It only
surfaced here because this is the first one whose view gates an action on membership role.

The fix is one parameter, defaulting to the previous behaviour:

```ts
mockAppBootstrap(page, communityId, membershipRole = 'MEMBER')
```

`community-projects` passes `"COORDINATOR"`. The default keeps every existing caller byte-identical,
which is why the gate still reports the same 86 passed.

## Two wrong hypotheses before the right one

The symptom suggested a render loop, so that was checked first, since round 67 turned up exactly that
in the dashboard. Measuring requests during a page load:

```
1  /api/auth/me
1  /api/help-center
1  /api/community/projects
1  /api/community/proposals
boton estable? presente
```

No loop, one request per route, button present. The deps `[activeCommunityId, t]` were stable. The
"not stable" part of Playwright's message was never about stability at all - it also waits for
**enabled**, and the button was disabled.

Recording this because two of the three hypotheses in this round were wrong, and the one that worked
came from reading the component rather than from more measurement.

## Where the test reaches now

Past board creation, the dropdown, and the submit. It now waits for
`project-task-move-forward-44444444-4444-4444-4444-444444444444`, so the next question is whether the
spec's task POST feeds back into the board the way the board POST does. The board mock was already
stateful (`let boards`, with the POST pushing into it), so this one may already be fine and the
blocker will be something else.

## Verification

`gate-run-ci-mode.txt` is the gate after the shared helper changed: **86 passed**, no failures, no
flaky, identical to round 75. Changing a helper that 16 specs depend on is exactly the kind of edit
that must be measured rather than assumed.

## Known limitations

- `community-projects` still fails, further along than before but not green.
- `community-proposals` still waits for a moderation control that never renders.
- `community-open-data` and `community-official-announcements` need a product decision, not a fix:
  whether token scopes should have per-scope test hooks, and which screen owns pinned announcements.
- The role downgrade existed in every spec converted with `mockAppBootstrap` between rounds 73 and
  75. Only this one gates an action on membership role, so it is the only one that showed a symptom.
  Others may be passing while asserting less than they intend.
- No production code was touched, so the evidence is test output rather than screenshots.