# Frontend + Backend Priority Backlog Top 20 (GSD 2026-03-07)

Detailed execution backlog for the highest-value work remaining in Open Civic Signal OS, with the latest shipped anchor stories retained below as delivery references.

## Prioritization Method

This pack prioritizes work using four filters:

1. civic trust impact
2. product loop completion from proposal -> decision -> execution -> public proof
3. user-facing usability leverage across multiple routes
4. backend contract ownership for deterministic behavior

## Priority Order Summary

1. `OCS-P1-042` verifiable community voting
2. `OCS-P1-049` moderation queue and sanction engine
3. `OCS-P1-050` privacy center and access logs
4. `OCS-P1-051` onboarding and help center
5. `OCS-P1-052` open civic data exports and scoped API
6. `OCS-P1-035` real-time rooms and mentions
7. `OCS-P1-044` volunteering and activity slots
8. `OCS-P1-045` shared resource booking
9. `OCS-P1-017` public shareability growth surfaces
10. `OCS-P1-021` notification preferences UX
11. `OCS-P1-022` funnel analytics instrumentation
12. `OCS-P1-016` low-bandwidth mode enforcement
13. `OCS-P1-018` remaining copy parity hardening
14. `OCS-P1-019` accessibility finish pass
15. `OCS-P1-053` decision-to-outcome public meeting pages
16. `P0 security` hardened compose/runtime secret policy
17. `P1 developer-experience` make preflight deterministic on host + Docker
18. `P1 trust-critical` align remaining active OpenAPI/examples
19. `P1 scalability` final paging/filter consistency sweep
20. `P1 observability` publish civic metrics-history trend snapshots

## Frontend Top 10

Reference note:

- `FE-01` and `FE-02` are now shipped anchor stories retained for continuity; the next remaining frontend priority is `FE-03`.

### FE-01 `story:OCS-P1-047` Build decision-ledger UI
- Rank: `F1`
- Why now: proposals, deliberation, governance docs, and project boards already exist; users still cannot see one trustworthy path from decision to execution.
- Product outcome: one community timeline that proves what was approved, when, why, and who owns delivery.
- Frontend scope:
  - add `/communities/decisions` route
  - render timeline cards linking proposal, governance source, vote result, and project board
  - expose filters for status, date, and decision type
  - add decision summary surface inside proposal and project detail views
- Acceptance criteria:
  - [x] decision ledger route exists and is navigable from community surfaces
  - [x] each decision card links to proposal, vote basis, and execution owner when present
  - [x] timeline stays legible on mobile without horizontal overflow
  - [x] empty and partial-link states are explicit
- Validation:
  - [x] Playwright decision-ledger browse flow
  - [ ] responsive evidence desktop + mobile

### FE-02 `story:OCS-P1-048` Public trust metrics dashboard UX
- Rank: `F2`
- Why now: the product needs visible proof of outcomes, not only workflows.
- Product outcome: communities and institutions can see whether the system is working.
- Frontend scope:
  - add public metrics dashboard route and card system
  - surface freshness, period filters, and drill-downs
  - standardize chart empty/error states for low-data communities
- Acceptance criteria:
  - [x] freshness timestamp is always visible
  - [x] metric cards explain what each metric means in plain language
  - [x] charts degrade gracefully under sparse data
  - [x] metrics route is usable in simple mode and advanced mode
- Validation:
  - [ ] Playwright metrics route checks
  - [ ] low-data snapshot evidence

### FE-03 `story:OCS-P1-042` Voting UX and result transparency
- Rank: `F3`
- Why now: proposals cannot become legitimate decisions without visible decision mechanics.
- Product outcome: one member, one visible decision trail.
- Frontend scope:
  - proposal vote panel with clear vote rule explanation
  - eligibility and verification state messaging
  - result tally surface with participation counts and status
- Acceptance criteria:
  - [ ] users understand if they can vote and why
  - [ ] tally/result view is readable and auditable
  - [ ] blocked-vote states explain next step
- Validation:
  - [ ] Playwright vote and tally flow

### FE-04 `story:OCS-P1-051` Onboarding and contextual help center
- Rank: `F4`
- Why now: the product surface is broad; users need guided meaning, not just more modules.
- Product outcome: lower abandonment and better first successful action per role.
- Frontend scope:
  - help center route
  - contextual help modules in report, proposals, governance, projects, communities
  - role-based onboarding entry points
- Acceptance criteria:
  - [ ] users can open help from key workflows without losing context
  - [ ] guides are bilingual and role-aware
  - [ ] help center search returns useful results for top civic workflows
- Validation:
  - [ ] Playwright contextual help journey

### FE-05 `story:OCS-P1-017` Shareability and growth surfaces
- Rank: `F5`
- Why now: product value is strong but still not easy to share externally.
- Product outcome: more community discovery and higher distribution of civic proof.
- Frontend scope:
  - social/meta cards for public signals, proposals, decisions, and metrics
  - public-friendly permalink layouts
  - copy-to-share actions with plain-language summaries
- Acceptance criteria:
  - [ ] public surfaces generate understandable preview metadata
  - [ ] share actions work from top civic objects
  - [ ] no private data leaks into public cards
- Validation:
  - [ ] Playwright public-share route checks

### FE-06 `story:OCS-P1-021` Notification preferences UX
- Rank: `F6`
- Why now: engagement loops exist but preferences are not explicit or trustworthy.
- Product outcome: users choose how the system re-engages them.
- Frontend scope:
  - preferences UI by channel and event type
  - digest frequency choices
  - community-specific follow controls
- Acceptance criteria:
  - [ ] users can opt into follow-ups by community/object type
  - [ ] notification states persist and render clearly
  - [ ] settings language remains plain, not technical
- Validation:
  - [ ] Playwright preference persistence checks

### FE-07 `story:OCS-P1-016` Low-bandwidth mode enforcement
- Rank: `F7`
- Why now: field/community leaders still need a lighter interface path.
- Product outcome: better completion under constrained devices and networks.
- Frontend scope:
  - low-bandwidth toggle and detected-mode hints
  - reduce non-critical visuals and payload-heavy surfaces
  - skeleton/empty states optimized for slow networks
- Acceptance criteria:
  - [ ] key actions remain usable under throttled network
  - [ ] heavy surfaces degrade without layout collapse
  - [ ] low-bandwidth mode stays coherent with design system
- Validation:
  - [ ] Playwright throttled-network evidence

### FE-08 `story:OCS-P1-018` Remaining EN/ES parity and content hardening
- Rank: `F8`
- Why now: new modules are shipping faster than language hardening.
- Product outcome: bilingual trust across governance-sensitive workflows.
- Frontend scope:
  - complete copy parity for governance, projects, future decision ledger, metrics, moderation
  - unify remaining helper/error labels
  - remove technical drift in UI text
- Acceptance criteria:
  - [ ] no mixed-language trust-critical surface
  - [ ] new modules ship EN/ES in same change set
  - [ ] long ES strings remain layout-safe
- Validation:
  - [ ] i18n parity script/report
  - [ ] Playwright EN/ES route spot checks

### FE-09 `story:OCS-P1-019` Accessibility finish pass
- Rank: `F9`
- Why now: many improvements landed, but new modules expanded the surface area.
- Product outcome: complete keyboard-first path across the current app breadth.
- Frontend scope:
  - governance, projects, proposals, maps, settings, auth, communities
  - focus order, aria labels, active nav, dialog semantics, table semantics
- Acceptance criteria:
  - [ ] all primary workflows are keyboard-completable
  - [ ] dialogs and dropdowns expose stable semantics
  - [ ] contrast issues are gone in both themes
- Validation:
  - [ ] Playwright keyboard-only scenarios
  - [ ] accessibility scan pass

### FE-10 Cross-app visual integrity hardening
- Rank: `F10`
- Why now: the design system is much stronger, but dense screens still need final normalization.
- Product outcome: lower cognitive load and fewer layout regressions as modules grow.
- Frontend scope:
  - unify data-heavy screens on shared primitives
  - eliminate remaining ad-hoc typography/surface patterns
  - stabilize tables, sidebars, and mobile action bars
- Acceptance criteria:
  - [ ] no visible text overlap in primary workflows
  - [ ] cards, badges, stats, filters, dialogs use shared primitives only
  - [ ] simple/advanced modes preserve consistent rhythm
- Validation:
  - [ ] Playwright screenshot pack desktop/mobile

## Backend Top 10

Reference note:

- `BE-01` and `BE-02` are now shipped anchor stories retained for continuity; the next remaining backend priority is `BE-03`.

### BE-01 `story:OCS-P1-047` Decision ledger domain and contract
- Rank: `B1`
- Why now: it completes the trust chain and unlocks metrics.
- Product outcome: formal decision record linked to proposal, vote, governance basis, and execution owner.
- Backend scope:
  - decision entity + migration
  - source linkage to proposal/vote/governance/project board
  - ledger list/detail endpoints
  - explicit lifecycle states for decisions
- Acceptance criteria:
  - [x] each decision links to one authoritative basis
  - [x] execution owner is visible when assigned
  - [x] records are community-scoped and auditable
- Validation:
  - [x] integration tests for linked-record retrieval
  - [x] OpenAPI + ADR update

### BE-02 `story:OCS-P1-048` Trust metrics aggregation API
- Rank: `B2`
- Why now: decision ledger and execution surfaces need public proof metrics.
- Product outcome: trustworthy public indicators for resolution, participation, and execution.
- Backend scope:
  - aggregate metrics endpoint(s)
  - period filters and freshness semantics
  - deterministic definitions for each metric
- Acceptance criteria:
  - [x] freshness is backend-owned
  - [x] metric formulas are deterministic and documented
  - [x] low-data communities return understandable payloads
- Validation:
  - [x] metrics contract tests
  - [x] reproducibility note in docs

### BE-03 `story:OCS-P1-042` Verifiable voting rules
- Rank: `B3`
- Why now: decision ledger without verifiable voting remains incomplete.
- Product outcome: defensible, auditable community decisions.
- Backend scope:
  - vote configuration model
  - one-person-one-vote enforcement
  - eligibility/verification checks
  - tally endpoints with audit trail
- Acceptance criteria:
  - [ ] duplicate voting is blocked deterministically
  - [ ] vote result and participation basis are exposed clearly
  - [ ] audit metadata captures actor, time, rule set
- Validation:
  - [ ] voting integration tests
  - [ ] OpenAPI + ADR update

### BE-04 `story:OCS-P1-049` Moderation queue and sanction engine
- Rank: `B4`
- Why now: more community surfaces increase moderation risk and abuse complexity.
- Product outcome: safer participation without opaque enforcement.
- Backend scope:
  - moderation report model
  - sanction ladder model
  - queue endpoints and appeal metadata
  - false-positive review path
- Acceptance criteria:
  - [ ] reported content enters auditable queue
  - [ ] sanctions are proportional and reversible where applicable
  - [ ] policy reason is always attached
- Validation:
  - [ ] moderation engine integration tests
  - [ ] abuse checklist review

### BE-05 `story:OCS-P1-050` Privacy center and sensitive-data access logs
- Rank: `B5`
- Why now: identity, governance, and decision features increase sensitivity.
- Product outcome: visible data responsibility and admin accountability.
- Backend scope:
  - access-log event model
  - privacy preference APIs
  - community open-data publication policy
- Acceptance criteria:
  - [ ] sensitive reads are logged with actor, scope, timestamp
  - [ ] privacy settings are backend-owned and auditable
  - [ ] open-data policy is community-scoped
- Validation:
  - [ ] privacy/access-log tests

### BE-06 `story:OCS-P1-052` Open data exports and scoped API
- Rank: `B6`
- Why now: institutional adoption requires export and integration surfaces.
- Product outcome: municipalities and universities can use the data without scraping.
- Backend scope:
  - CSV/JSON exports
  - API token scopes and rate limits
  - export audit logs
- Acceptance criteria:
  - [ ] exports exist for signals, proposals, decisions, metrics
  - [ ] privileged exports are logged
  - [ ] scope/rate limit behavior is explicit
- Validation:
  - [ ] export contract tests
  - [ ] external-consumer examples in OpenAPI/docs

### BE-07 `story:OCS-P1-035` Real-time community rooms and mentions
- Rank: `B7`
- Why now: async layers are strong; real-time coordination is the next operational leap.
- Product outcome: communities coordinate active work without leaving the system.
- Backend scope:
  - websocket room events
  - mention and mute state
  - delivery rules and audit events
- Acceptance criteria:
  - [ ] room events are community-scoped
  - [ ] mentions create deterministic notification events
  - [ ] mute state is user-specific and persistent
- Validation:
  - [ ] websocket integration tests

### BE-08 `story:OCS-P1-044` Volunteer/activity slot engine
- Rank: `B8`
- Why now: project boards need human participation flows.
- Product outcome: communities can recruit real participation, not just tasks.
- Backend scope:
  - activity, slot, signup, attendance models
  - cap enforcement and reminder hooks
- Acceptance criteria:
  - [ ] slots enforce capacity
  - [ ] users can join/leave under rule windows
  - [ ] organizer can query roster and fill rate
- Validation:
  - [ ] signup rule integration tests

### BE-09 `story:OCS-P1-045` Shared resource booking engine
- Rank: `B9`
- Why now: communities need coordinated use of places/equipment once operations mature.
- Product outcome: fairer allocation of common resources.
- Backend scope:
  - resource, booking, approval, conflict rules
  - policy-aware availability endpoints
- Acceptance criteria:
  - [ ] double booking is blocked or escalated deterministically
  - [ ] requester can see approval state and reason
  - [ ] policies are explicit per resource/community
- Validation:
  - [ ] booking conflict tests

### BE-10 Platform hardening pack
- Rank: `B10`
- Why now: scale and trust are now constrained more by operational reliability than missing tables.
- Backend scope:
  - remove remaining compose/runtime security debt
  - finish OpenAPI parity for active routes/examples
  - make `agent:preflight` and host/Docker validation more deterministic
  - finish paging/filter consistency on remaining community endpoints
- Acceptance criteria:
  - [ ] compose runtime no longer depends on insecure defaults
  - [ ] active API routes have aligned contract examples
  - [ ] community endpoints share deterministic paging/filter semantics
  - [ ] local verification path is documented and repeatable
- Validation:
  - [ ] security/runtime checks
  - [ ] OpenAPI parity checks
  - [ ] CI/local validation docs

## Recommended Execution Waves

### Wave 1: Trust Loop Completion
1. `BE-01 / FE-01` decision ledger
2. `BE-03 / FE-03` verifiable voting
3. `BE-02 / FE-02` trust metrics

### Wave 2: Safety and Data Responsibility
4. `BE-04` moderation engine
5. `BE-05` privacy center backend
6. `FE-06` notification preferences
7. `FE-09` accessibility finish pass

### Wave 3: Operational Growth
8. `BE-07 / FE-04` realtime + help surfaces
9. `BE-08` volunteer slots
10. `BE-09` resource booking
11. `BE-06` exports and scoped API

### Wave 4: Reach and Hardening
12. `FE-05` shareability
13. `FE-07` low-bandwidth mode
14. `FE-08` EN/ES parity hardening
15. `FE-10` visual integrity hardening
16. `BE-10` platform hardening pack

## Delivery Notes

- Backend should continue owning all ranking, governance, decision, and metric calculations.
- Frontend should continue focusing on clarity, traceability, and low-friction execution workflows.
- New work should be seeded into issue execution in this order unless a trust-critical blocker overrides it.
