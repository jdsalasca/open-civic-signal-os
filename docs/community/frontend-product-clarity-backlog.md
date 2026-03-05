# Frontend Product Clarity Backlog (GSD 2026-03-05)

Spec-driven backlog to make Open Civic Signal OS feel simpler, more valuable, and easier to act on for first-time and low-context users.

## Why This Wave Exists

Observed friction from repo docs and current app behavior:

- first screens expose too many choices before users understand the main job-to-be-done
- navigation competes with the primary civic action instead of guiding it
- role/workflow distinctions are real, but not always explained in plain language
- README explains the stack well, but not the user promise, first action, or expected outcome clearly enough

## Product Outcome

- A first-time user should understand the value of the product and the next best action within 10-15 seconds.

## Execution Order

1. `story:OCS-P1-023` simplify dashboard action hierarchy and reduce first-view choice overload
2. `story:OCS-P1-024` ship role-aware guided home with one primary next step
3. `story:OCS-P1-025` reduce navigation complexity with progressive disclosure and clearer labels
4. `story:OCS-P1-026` strengthen report-to-follow-up trust loop with explicit "what happens next"
5. `story:OCS-P1-027` make empty, loading, and no-access states action-oriented and reassuring
6. `story:OCS-P1-028` rewrite public-facing README/landing copy around user value instead of repo structure

## Story Pack

### `story:OCS-P1-023` Dashboard Choice Overload Reduction

- Civic objective: increase first-session action rate by making the dashboard easier to parse.
- Feature mapping:
  - API endpoint change: none required initially.
  - Contract update: none required initially.
  - UI workflow update: reduce simultaneous hero actions, group secondary actions behind lower-emphasis surfaces, and clarify the single primary CTA.
- Acceptance criteria:
  - [ ] First dashboard viewport exposes one dominant primary action.
  - [ ] Secondary actions are still available but visually de-prioritized.
  - [ ] New users can identify where to report vs where to browse vs where to track contributions.
- Validation:
  - [ ] Playwright screenshot and keyboard pass for dashboard.
  - [ ] Before/after UI audit notes on action density and hierarchy.

### `story:OCS-P1-024` Role-Aware Guided Home

- Civic objective: help each user type understand the best next step immediately.
- Feature mapping:
  - API endpoint change: optional future endpoint for recommended next actions by role.
  - Contract update: optional future guided-home card schema.
  - UI workflow update: dashboard hero or welcome block adapts to citizen/moderator/public-servant intent with one recommended next action.
- Acceptance criteria:
  - [ ] Citizen sees report/support/follow guidance.
  - [ ] Moderator sees moderation/community-health guidance.
  - [ ] Public servant sees publish/update/resolve guidance.
- Validation:
  - [ ] Playwright route checks across role variants.
  - [ ] Copy review for EN/ES parity.

### `story:OCS-P1-025` Navigation Simplification and Progressive Disclosure

- Civic objective: reduce cognitive load and route confusion.
- Feature mapping:
  - API endpoint change: none.
  - Contract update: none.
  - UI workflow update: simplify labels, reduce duplicate affordances, and progressively reveal advanced/community management options.
- Acceptance criteria:
  - [ ] Main navigation labels are plain-language and non-overlapping.
  - [ ] High-frequency user actions remain visible without overwhelming first-time users.
  - [ ] Mobile and desktop navigation expose equivalent meaning with fewer competing choices.
- Validation:
  - [ ] Playwright desktop/mobile nav audit.
  - [ ] Keyboard-only navigation pass.

### `story:OCS-P1-026` Report-to-Outcome Trust Loop

- Civic objective: help users understand what happens after submitting a report so the product feels useful, not opaque.
- Feature mapping:
  - API endpoint change: optional future report lifecycle status summary endpoint.
  - Contract update: optional lifecycle explanation block schema.
  - UI workflow update: after reporting, users see plain-language explanation of review, prioritization, support, and follow-up path.
- Acceptance criteria:
  - [ ] Report flow explains the next system states in plain language.
  - [ ] Follow-up entry points are visible after successful report creation.
  - [ ] Users can find their contribution trail without hunting through navigation.
- Validation:
  - [ ] Playwright report success journey.
  - [ ] Copy parity check for EN/ES lifecycle explanation.

### `story:OCS-P1-027` Action-Oriented Empty and Restricted States

- Civic objective: stop the app from feeling broken or intimidating when users hit empty/no-access scenarios.
- Feature mapping:
  - API endpoint change: none.
  - Contract update: none.
  - UI workflow update: every empty or restricted state gives clear explanation plus one useful next action.
- Acceptance criteria:
  - [ ] Empty states explain why there is no data yet.
  - [ ] Restricted states explain role/community requirement without jargon.
  - [ ] Every empty/restricted state has a useful recovery or next action.
- Validation:
  - [ ] Playwright checks on report/blog/thread/dashboard empty and restricted paths.
  - [ ] Accessibility pass on action buttons and headings.

### `story:OCS-P1-028` Public-Facing Product Narrative Refresh

- Civic objective: make the product understandable before a user enters the app or reads repo setup docs.
- Feature mapping:
  - API endpoint change: none.
  - Contract update: none.
  - UI workflow update: README and future landing/public surfaces explain value, promise, and first action in user language.
- Acceptance criteria:
  - [ ] README explains the user promise before the technical stack.
  - [ ] Public-facing copy clarifies who the product is for and what users can do first.
  - [ ] Documentation aligns with the simpler frontend experience being implemented.
- Validation:
  - [ ] README/product copy review.
  - [ ] Consistency check against app navigation and top-level CTAs.
