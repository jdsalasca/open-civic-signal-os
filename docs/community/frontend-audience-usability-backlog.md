# Frontend Audience and Usability Backlog (GSD 2026-03-04)

Spec-driven backlog to increase civic reach, activation, and retention through frontend UX improvements with measurable outcomes.

## North-Star Outcome

- Increase active community participation by reducing onboarding friction and improving everyday usability in low-bandwidth and mobile-first conditions.

## Frontend Priority Stack (Execution Order)

1. `story:OCS-P1-015` onboarding conversion funnel hardening.
2. `story:OCS-P1-016` mobile low-bandwidth performance pack.
3. `story:OCS-P1-017` public reach and shareability growth surfaces.
4. `story:OCS-P1-018` plain-language EN/ES copy and comprehension pass.
5. `story:OCS-P1-019` accessibility baseline uplift (keyboard/screen-reader).
6. `story:OCS-P1-020` trust explainability UX for first-time visitors.
7. `story:OCS-P1-021` re-engagement reminders and notification preference UX.
8. `story:OCS-P1-022` product analytics instrumentation for funnel drop-off diagnosis.

## Story Pack

### `story:OCS-P1-015` Funnel Hardening for Sign-Up and First Action

- Civic objective: reduce abandonment from registration to first report/post interaction.
- Feature mapping:
  - API endpoint change: expose deterministic registration/drop-off status telemetry endpoint.
  - Contract update: event schema for `onboarding_step_completed`.
  - UI workflow update: guided progress states for register -> verify -> first action.
- Acceptance criteria:
  - [ ] Registration and verify screens expose explicit step progress and recovery cues.
  - [ ] Time-to-first-action median improves in pilot cohort vs baseline week.
  - [ ] Drop-off reasons are captured in auditable analytics events.
- Validation:
  - [ ] Playwright desktop/mobile flow from register to first report.
  - [ ] Event contract tests for onboarding telemetry payload.

### `story:OCS-P1-016` Mobile Low-Bandwidth UX and Payload Budget

- Civic objective: improve successful sessions for field leaders on unstable networks.
- Feature mapping:
  - API endpoint change: support compact response mode for dashboard/feed lists.
  - Contract update: query param + response profile for compact mode.
  - UI workflow update: progressive loading skeletons and deferred non-critical assets.
- Acceptance criteria:
  - [ ] Initial route payload for dashboard/feed reduced by agreed budget target.
  - [ ] Critical actions (report, vote, comment) remain responsive under throttled network.
  - [ ] No visual regressions in mobile breakpoint test suite.
- Validation:
  - [ ] Playwright trace with throttled network profile.
  - [ ] Build report showing JS/CSS budget deltas.

### `story:OCS-P1-017` Public Reach and Shareability Surfaces

- Civic objective: increase audience acquisition from social/community channels.
- Feature mapping:
  - API endpoint change: expose public sharing metadata for top civic signals.
  - Contract update: share card schema (title, summary, freshness, canonical URL).
  - UI workflow update: share CTA + copy-link action on public problem cards.
- Acceptance criteria:
  - [ ] Public cards include human-readable summaries and share action.
  - [ ] Shared links resolve to accessible, context-rich public views.
  - [ ] Trackable share events are emitted for growth analysis.
- Validation:
  - [ ] Playwright coverage for share CTA and link resolve behavior.
  - [ ] Contract tests for share metadata endpoint.

### `story:OCS-P1-018` Plain-Language EN/ES Comprehension Pass

- Civic objective: improve comprehension for mixed literacy and bilingual audiences.
- Feature mapping:
  - API endpoint change: localized server error/help copy keys for trust-critical actions.
  - Contract update: localized message key catalog contract.
  - UI workflow update: harmonized plain-language labels, placeholders, and helper text.
- Acceptance criteria:
  - [ ] Trust-critical forms include equivalent EN/ES guidance and char-counter hints.
  - [ ] No technical jargon in citizen-facing defaults unless role-specific.
  - [ ] Language switch preserves route context and current form input safely.
- Validation:
  - [ ] Playwright EN/ES snapshot checks on login/register/report/blog/thread flows.
  - [ ] i18n key parity check with no missing translations in touched views.

### `story:OCS-P1-019` Accessibility Baseline Uplift

- Civic objective: remove barriers for keyboard-first and assistive-tech users.
- Feature mapping:
  - API endpoint change: N/A (UI-first).
  - Contract update: component accessibility contract for focus/landmark behavior.
  - UI workflow update: robust skip links, focus-visible states, semantic landmarks, `aria-current`.
- Acceptance criteria:
  - [ ] Core routes are navigable end-to-end without mouse.
  - [ ] Main nav, forms, and dialogs expose clear focus order and labels.
  - [ ] Contrast/token usage passes agreed accessibility checks.
- Validation:
  - [ ] Playwright keyboard path script across core routes.
  - [ ] Automated accessibility scan report for critical pages.

### `story:OCS-P1-020` Trust Explainability for New Visitors

- Civic objective: increase confidence in ranking fairness for first-time users.
- Feature mapping:
  - API endpoint change: include concise scoring rationale snippet in list responses.
  - Contract update: list-item explainability summary fields.
  - UI workflow update: "why ranked here" preview on cards and detail jump.
- Acceptance criteria:
  - [ ] Top backlog cards expose short explainability reasons before click-through.
  - [ ] Formula and data freshness visibility remains clear on dashboard surfaces.
  - [ ] User testing indicates improved understanding of ranking rationale.
- Validation:
  - [ ] API integration tests for explainability snippet fields.
  - [ ] Playwright assertions for visible explainability block in list/detail views.

### `story:OCS-P1-021` Re-Engagement and Notification Preference UX

- Civic objective: improve returning-user participation in ongoing community discussions.
- Feature mapping:
  - API endpoint change: notification preference CRUD endpoint.
  - Contract update: preference schema for channels/frequency.
  - UI workflow update: settings panel for reminder frequency and topic subscriptions.
- Acceptance criteria:
  - [ ] Users can set and persist notification preferences without losing context.
  - [ ] Reminder eligibility respects role, channel, and consent settings.
  - [ ] Community/thread return rate improves in pilot cohort.
- Validation:
  - [ ] API tests for preference persistence and validation rules.
  - [ ] Playwright settings flow with persistence assertions after route transitions.

### `story:OCS-P1-022` Frontend Funnel Analytics Instrumentation

- Civic objective: identify real UX bottlenecks with auditable metrics.
- Feature mapping:
  - API endpoint change: ingestion endpoint for frontend analytics batches.
  - Contract update: typed analytics event schema with source/timestamp/version.
  - UI workflow update: instrumentation hooks on key conversion and error paths.
- Acceptance criteria:
  - [ ] Conversion funnel events are emitted for onboarding, report, and engagement actions.
  - [ ] Events include community context + role safely without sensitive payload leakage.
  - [ ] Dashboard/report can segment drop-offs by device and route.
- Validation:
  - [ ] Contract tests for analytics payload schema and rejected invalid events.
  - [ ] Playwright scenario asserting event fire on critical UX paths.

## Suggested Milestones

- `milestone:frontend-audience-sprint-1` -> `OCS-P1-015`, `OCS-P1-016`, `OCS-P1-018`
- `milestone:frontend-audience-sprint-2` -> `OCS-P1-017`, `OCS-P1-019`, `OCS-P1-020`
- `milestone:frontend-audience-sprint-3` -> `OCS-P1-021`, `OCS-P1-022`

## Labels

- `impact-high` for stories with measurable participation gain in <30 days.
- `trust-critical` for explainability/accessibility/onboarding trust paths.
- `pilot-ready` for stories testable with one real community partner.
