# Community Operating System Backlog (GSD 2026-03-05)

Spec-driven expansion backlog to turn Open Civic Signal OS into a high-trust community operating system for neighborhoods, universities, civic groups, and local institutions.

## Product Thesis

The current app already supports reporting, prioritization, community context, and accountability signals. The next product step is not "more screens"; it is a coherent operating model across five layers:

1. identity and spaces
2. communication and social fabric
3. decisions and case handling
4. execution and coordination
5. transparency and trust

## Design Guardrails

- Keep ranking logic backend-owned and auditable.
- Treat privacy, verification, and moderation as first-order product concerns, not add-ons.
- Use simple mode by default; progressively disclose advanced governance controls.
- Every governance-sensitive feature must declare:
  - backend/API ownership
  - contract impact
  - UI workflow impact
  - measurable civic outcome
  - validation evidence

## Execution Sequence

### Wave A: Identity and Trust Foundations

1. `story:OCS-P1-029` enriched civic identity profiles with privacy visibility controls
2. `story:OCS-P1-030` hierarchical communities with tree navigation and breadcrumbs
3. `story:OCS-P1-031` community-scoped RBAC with configurable scopes
4. `story:OCS-P1-032` simple vs advanced experience modes

### Wave B: Communication Surfaces

5. `story:OCS-P1-033` official announcements channel
6. `story:OCS-P1-034` nested forum threads with relevance ordering
7. `story:OCS-P1-035` real-time rooms, mentions, and thread mute controls
8. `story:OCS-P1-036` unified community home across announcements, discussions, and chat presence

### Wave C: Casework and Deliberation

9. `story:OCS-P1-037` issue tracker-grade report wizard
10. `story:OCS-P1-038` case timeline, assignment, and lifecycle audit trail
11. `story:OCS-P1-039` geospatial issue map with clustering and filters
12. `story:OCS-P1-040` structured proposal templates
13. `story:OCS-P1-041` pro/con deliberation and evidence markers
14. `story:OCS-P1-042` verifiable community voting and prioritization rules

### Wave D: Execution and Shared Operations

15. `story:OCS-P1-043` community project boards with micro-kanban
16. `story:OCS-P1-044` volunteering signup, slots, and reminders
17. `story:OCS-P1-045` shared resource booking calendars and approval rules

### Wave E: Governance and Public Trust

18. `story:OCS-P1-046` governance library for rules, minutes, budgets, and agreements
19. `story:OCS-P1-047` community decision ledger linked to proposals and execution
20. `story:OCS-P1-048` public trust metrics dashboards
21. `story:OCS-P1-049` moderation queues and sanction engine
22. `story:OCS-P1-050` privacy center and sensitive-data access logs
23. `story:OCS-P1-051` civic onboarding, tours, and help center
24. `story:OCS-P1-052` open civic data exports and scoped external API
25. `story:OCS-P1-053` outbound integrations layer

### Strategic Bets After Foundations

26. `story:OCS-P2-006` verified identity tiers for sensitive actions
27. `story:OCS-P2-007` auditable secure elections
28. `story:OCS-P2-008` participatory budgeting allocation
29. `story:OCS-P2-009` bounded civic reputation model

## Story Pack

### `story:OCS-P1-029` Enriched Civic Identity Profiles

- Problem: users participate with too little context or too much forced exposure, which weakens trust and safety.
- Backend/API change:
  - add profile fields for role, affiliations, verification level, and visibility scopes
  - expose deterministic audience-filtered profile payloads
- Contract change:
  - extend user/community profile schemas and privacy enum values
- UI workflow:
  - editable profile card with explicit visibility choices: public, community-only, admins-only
- Civic outcome:
  - people can signal who they are in context without oversharing
- Acceptance criteria:
  - [ ] profile fields support role plus multiple affiliations
  - [ ] visibility scope is configurable per field group
  - [ ] unauthorized viewers never receive hidden profile fields
- Validation:
  - [ ] backend privacy filter tests
  - [ ] Playwright profile visibility checks across viewer roles

### `story:OCS-P1-030` Hierarchical Communities and Breadcrumbs

- Problem: large universities and city communities need nested spaces, not a flat list.
- Backend/API change:
  - introduce parent-child community relationships and scoped listing endpoints
- Contract change:
  - add hierarchy tree and breadcrumb response models
- UI workflow:
  - tree selector, scoped landing pages, breadcrumb navigation
- Civic outcome:
  - users can orient from umbrella entity to local working group quickly
- Acceptance criteria:
  - [ ] community can have nested subgroups
  - [ ] API returns parent, children, and breadcrumb path
  - [ ] route transitions preserve current hierarchy context
- Validation:
  - [ ] hierarchy contract tests
  - [ ] Playwright tree navigation and breadcrumb persistence checks

### `story:OCS-P1-031` Community-Scoped RBAC Policies

- Problem: governance actions need flexible local permissions instead of one global role flag.
- Backend/API change:
  - enforce scopes for create proposal, publish official update, moderate, view sensitive data, manage resources
- Contract change:
  - document permission matrix and deterministic `403` reason payload
- UI workflow:
  - role policy admin screen and inline permission explanations where actions are blocked
- Civic outcome:
  - communities can govern safely without opaque authorization behavior
- Acceptance criteria:
  - [ ] role assignments are scoped to community
  - [ ] permission checks are auditable
  - [ ] blocked actions return machine-readable and user-readable reason
- Validation:
  - [ ] authorization matrix tests
  - [ ] OpenAPI permission/error review
  - [ ] Playwright blocked-action rationale checks

### `story:OCS-P1-032` Simple vs Advanced Experience Modes

- Problem: the same UI cannot optimally serve first-time citizens and power users.
- Backend/API change:
  - none initially; preference persisted in user settings
- Contract change:
  - add user preference field for interface mode
- UI workflow:
  - default simple mode with guided actions, calmer first-view hierarchy, and a standardized community-first visual shell
  - advanced mode reveals filters, analytics, and admin utilities without breaking the shared design system
- Civic outcome:
  - broader usability across tech comfort levels
- Acceptance criteria:
  - [ ] simple mode hides non-essential complexity
  - [ ] first-view shell uses a consistent identity system across navigation, cards, badges, and high-impact pages
  - [ ] advanced mode preserves full governance tooling
  - [ ] preference persists across sessions and devices
- Validation:
  - [ ] settings persistence tests
  - [ ] Playwright simple/advanced route comparisons

### `story:OCS-P1-033` Official Announcements Channel

- Problem: official communications are currently mixed with general participation surfaces.
- Backend/API change:
  - add announcement post type with pinning, archive, and official-author constraints
- Contract change:
  - add announcement schema with `official`, `pinned`, and archive metadata
- UI workflow:
  - dedicated official updates view with clear label and archive search
- Civic outcome:
  - institutional updates are easier to trust and track
- Acceptance criteria:
  - [ ] only allowed roles can publish official announcements
  - [ ] pinned announcements remain visible at top
  - [ ] archive is searchable by community and date
- Validation:
  - [ ] backend create/list/archive tests
  - [ ] Playwright official channel audit

### `story:OCS-P1-034` Nested Forum Threads with Relevance Ordering

- Problem: flat discussions scale poorly and bury the most useful exchanges.
- Backend/API change:
  - extend thread/comment model for nested replies and relevance ordering inputs
- Contract change:
  - add nested reply and relevance sort query support
- UI workflow:
  - Reddit-like threaded discussions with reply depth, reactions, attachments
- Civic outcome:
  - high-signal community conversations stay usable as volume grows
- Acceptance criteria:
  - [ ] replies can nest to defined depth
  - [ ] ideas/debates sort by freshness plus engagement
  - [ ] official announcements remain chronological
- Validation:
  - [ ] backend thread ordering tests
  - [ ] Playwright discussion tree interactions

### `story:OCS-P1-035` Real-Time Community Rooms

- Problem: communities need lightweight coordination beyond async threads.
- Backend/API change:
  - websocket-backed room events, direct message primitives, mention and mute state
- Contract change:
  - document room/message/presence event shapes
- UI workflow:
  - room list, thread mute, mention highlights, basic direct messaging
- Civic outcome:
  - working groups can coordinate quickly during active issues or events
- Acceptance criteria:
  - [ ] rooms exist per community or project group
  - [ ] users can mention others and mute noisy threads
  - [ ] notification state is user-specific and auditable
- Validation:
  - [ ] websocket integration tests
  - [ ] Playwright real-time smoke flow

### `story:OCS-P1-036` Unified Community Home

- Problem: users need one coherent landing page instead of disconnected surfaces.
- Backend/API change:
  - aggregate official updates, hot discussions, active chat rooms, and top open issues
- Contract change:
  - add community-home composite response with freshness metadata
- UI workflow:
  - unified community home with progressive disclosure and clear section hierarchy
- Civic outcome:
  - users understand community pulse without hunting across routes
- Acceptance criteria:
  - [ ] home shows official, discussion, and action surfaces without overload
  - [ ] each section links to deeper workflow
  - [ ] freshness timestamp is visible
- Validation:
  - [ ] composite endpoint tests
  - [ ] Playwright first-view comprehension audit

### `story:OCS-P1-037` Issue Tracker-Grade Reporting Flow

- Problem: report forms need stronger structure to support routing and institutional action.
- Backend/API change:
  - extend issue schema for category, impact, location, attachments, and optional geodata
- Contract change:
  - document multi-step create payloads and attachment/location fields
- UI workflow:
  - 2-3 step wizard with inline validation and optional geolocation
- Civic outcome:
  - higher quality case intake with less user confusion
- Acceptance criteria:
  - [ ] title, category, impact, and description are guided and validated
  - [ ] multiple evidence attachments are supported
  - [ ] geolocation is optional and clearly explained
- Validation:
  - [ ] backend create/update tests
  - [ ] Playwright wizard flow

### `story:OCS-P1-038` Case Timeline and Assignment Trail

- Problem: once a report exists, people need to see movement and responsibility.
- Backend/API change:
  - add lifecycle events, assignee references, comments, and state transition rules
- Contract change:
  - add timeline event schema and assignment payloads
- UI workflow:
  - visual case timeline with comments, responsible parties, and status history
- Civic outcome:
  - every case becomes traceable from intake to resolution
- Acceptance criteria:
  - [ ] timeline shows who changed status and when
  - [ ] allowed transitions are deterministic
  - [ ] assignment changes are visible and auditable
- Validation:
  - [ ] backend lifecycle tests
  - [ ] Playwright detail timeline checks

### `story:OCS-P1-039` Geospatial Issue Map

- Problem: communities need geographic pattern visibility, not just lists.
- Backend/API change:
  - add one community map endpoint plus one cross-community heat endpoint with filter echo and cluster-ready payloads
- Contract change:
  - document community point/cluster and cross-community heat-cell response shapes
- UI workflow:
  - active-community map with hotspots, federated heat across visible communities, real filters, and click-through to issue detail
- Civic outcome:
  - spatial hotspots become easier to detect and act on
- Acceptance criteria:
  - [ ] every active community has a dedicated issue map surface
  - [ ] cross-community heat view shows where pressure is building between communities
  - [ ] map filters by category, status, and date
  - [ ] clustered view remains usable on mobile
  - [ ] issue click opens detail context cleanly
- Validation:
  - [ ] geo endpoint tests
  - [ ] Playwright map filter flow

### `story:OCS-P1-040` Structured Proposal Templates

- Problem: proposals are hard to compare when every author uses a different format.
- Backend/API change:
  - add proposal domain with template-backed required sections
- Contract change:
  - define proposal create/update/detail schemas
- UI workflow:
  - guided proposal form for problem, solution, cost, beneficiaries
- Civic outcome:
  - communities can compare and prioritize proposals more fairly
- Acceptance criteria:
  - [x] templates enforce minimum structure
  - [x] proposals link to related issues or documents
  - [x] detail view clearly separates problem, solution, and cost logic
- Validation:
  - [x] backend proposal tests
  - [x] Playwright create/detail flow

### `story:OCS-P1-041` Structured Deliberation and Evidence

- Problem: flat comments do not support thoughtful public reasoning.
- Backend/API change:
  - add typed deliberation entries: pro, con, question, evidence
- Contract change:
  - document deliberation types and counters
- UI workflow:
  - visually separated pro/con sections with evidence markers
- Civic outcome:
  - decisions can be debated with more signal and less noise
- Acceptance criteria:
  - [x] arguments are typed and countable
  - [x] evidence can be marked and surfaced distinctly
  - [x] moderation can act on abuse without deleting the whole proposal context
- Validation:
  - [x] backend deliberation tests
  - [x] Playwright pro/con evidence interactions

### `story:OCS-P1-042` Verifiable Community Voting

- Problem: proposals need fair and inspectable decision mechanisms.
- Backend/API change:
  - add vote rules, one-person-one-vote guardrails, public tally views
- Contract change:
  - document vote config, tally, and verification metadata
- UI workflow:
  - simple vote or scored vote, visible participation totals, community filters
- Civic outcome:
  - communities can make formal or semi-formal decisions with higher trust
- Acceptance criteria:
  - [x] vote limits are enforced deterministically
  - [x] results are public at the configured level
  - [x] anti-abuse checks are auditable
- Validation:
  - [x] backend voting tests
  - [ ] Playwright vote/tally flow

### `story:OCS-P1-043` Community Project Boards

- Problem: approved work needs an execution surface, not just a decision record.
- Backend/API change:
  - add project/task entities with statuses, owners, due dates, and comments
- Contract change:
  - define board/task schemas and update events
- UI workflow:
  - micro-kanban per project with assignment and comment thread
- Civic outcome:
  - communities can see execution, not just intention
- Acceptance criteria:
  - [x] tasks move across pending, in progress, done
  - [x] every task has owner and due date support
  - [x] boards can link back to decision or issue origin
- Validation:
  - [x] backend project/task tests
  - [x] Playwright kanban interactions

### `story:OCS-P1-044` Volunteering and Activity Slots

- Problem: many community actions require people and time slots, not only comments.
- Backend/API change:
  - add activity signup slots, caps, reminders, and attendance records
- Contract change:
  - define activity/slot/signup schemas
- UI workflow:
  - sign-up flow with capacity feedback and volunteer list
- Civic outcome:
  - communities can organize real-world participation faster
- Acceptance criteria:
  - [ ] activities support slots and capacity
  - [ ] users can join and leave within rule windows
  - [ ] organizer sees roster and fill rate
- Validation:
  - [ ] backend signup tests
  - [ ] Playwright volunteer flow

### `story:OCS-P1-045` Shared Resource Booking

- Problem: community spaces and equipment need fair coordination.
- Backend/API change:
  - add reservable resources, approval state, and conflict rules
- Contract change:
  - define booking request/approval schemas
- UI workflow:
  - booking calendar with rules, conflict feedback, and approval status
- Civic outcome:
  - shared resources are allocated more transparently
- Acceptance criteria:
  - [ ] resources expose availability and booking policies
  - [ ] conflicts are prevented or clearly escalated
  - [ ] approval flow is visible to requester
- Validation:
  - [ ] backend booking tests
  - [ ] Playwright calendar booking flow

### `story:OCS-P1-046` Governance Library

- Problem: rules, acts, and agreements are often scattered or inaccessible.
- Backend/API change:
  - add categorized document repository with versions and search metadata
- Contract change:
  - define document, category, version, and visibility schemas
- UI workflow:
  - searchable governance library and simple public meeting page pattern
- Civic outcome:
  - communities can find the rules before conflict or misinformation spreads
- Acceptance criteria:
  - [x] documents are searchable by type and community
  - [x] versions remain accessible
  - [x] visibility rules are explicit
- Validation:
  - [x] backend search/version tests
  - [x] Playwright document discovery flow

### `story:OCS-P1-047` Community Decision Ledger

- Problem: decisions are hard to trace once proposals move into execution.
- Backend/API change:
  - add decision records linked to proposal, vote result, assignees, and status
- Contract change:
  - define decision ledger schema and linked entity references
- UI workflow:
  - community timeline showing what was approved, when, by whom, and who executes it
- Civic outcome:
  - communities can audit the path from debate to execution
- Acceptance criteria:
  - [x] each decision links to source proposal or record
  - [x] vote or approval basis is visible
  - [x] execution owner is visible where applicable
- Validation:
  - [x] backend decision-link tests
  - [x] Playwright decision timeline audit

### `story:OCS-P1-048` Public Trust Metrics Dashboard

- Problem: a civic system without public metrics cannot prove progress.
- Backend/API change:
  - aggregate resolution, participation, execution, and timeliness indicators
- Contract change:
  - define metric cards, series, filters, and freshness metadata
- UI workflow:
  - simple public dashboard with drill-downs by community and period
- Civic outcome:
  - users can see whether the system is producing outcomes
- Acceptance criteria:
  - [x] dashboard shows freshness and time period
  - [x] metrics are explainable and filterable
  - [x] low-data states remain understandable
- Validation:
  - [x] backend metrics tests
  - [ ] Playwright dashboard checks

### `story:OCS-P1-049` Moderation Queues and Sanctions

- Problem: participation surfaces need proportionate moderation and review workflows.
- Backend/API change:
  - add content reports, moderation queue, action history, sanction ladder
- Contract change:
  - define moderation report and enforcement schemas
- UI workflow:
  - moderator queue with hide, warn, suspend, and appeal indicators
- Civic outcome:
  - trust and safety improve without hidden arbitrary moderation
- Acceptance criteria:
  - [ ] content can be reported with reason
  - [ ] sanctions are proportional and auditable
  - [ ] moderation actions do not erase traceability
- Validation:
  - [ ] backend moderation tests
  - [ ] Playwright moderator queue flow

### `story:OCS-P1-050` Privacy Center and Access Logs

- Problem: communities need clarity on what data is visible and who accessed sensitive information.
- Backend/API change:
  - add privacy preferences, open-data publication controls, and access log events
- Contract change:
  - define privacy preference and access-log schemas
- UI workflow:
  - privacy center for profile/activity visibility and data publication controls
- Civic outcome:
  - users gain confidence that the system handles data responsibly
- Acceptance criteria:
  - [ ] user can control visibility of profile and activity
  - [ ] community admins can define open-data publication policy
  - [ ] sensitive data access is logged and reviewable
- Validation:
  - [ ] backend privacy/access-log tests
  - [ ] Playwright privacy center checks

### `story:OCS-P1-051` Civic Onboarding and Help Center

- Problem: users need guidance on how to participate well, not just where to click.
- Backend/API change:
  - optional help/article content endpoints and onboarding completion state
- Contract change:
  - define onboarding step and help article schemas
- UI workflow:
  - guided tours, FAQs, and context-sensitive "how this works" modules
- Civic outcome:
  - first-time participation improves and misuse decreases
- Acceptance criteria:
  - [ ] onboarding exists for citizens, moderators, and representatives
  - [ ] help content is searchable and bilingual
  - [ ] users can dismiss and revisit guides
- Validation:
  - [ ] onboarding state tests
  - [ ] Playwright walkthrough/help flow

### `story:OCS-P1-052` Open Civic Data Exports and Scoped API

- Problem: universities and municipalities need interoperable exports and integration surfaces.
- Backend/API change:
  - add scoped exports and public/private API access controls with rate limits
- Contract change:
  - publish export and API auth/rate-limit contract details
- UI workflow:
  - export center for CSV/JSON and API token guidance for authorized roles
- Civic outcome:
  - communities can build local dashboards and external analysis without scraping
- Acceptance criteria:
  - [ ] exports exist for issues, proposals, votes, decisions, and metrics
  - [ ] scope and rate limits are explicit
  - [ ] audit trail exists for privileged data export
- Validation:
  - [ ] export integration tests
  - [ ] contract examples for external consumers

### `story:OCS-P1-053` Outbound Integrations Layer

- Problem: community operations depend on calendars, messaging, email, and maps.
- Backend/API change:
  - add webhook and connector abstraction for external delivery and sync jobs
- Contract change:
  - define integration config and webhook event models
- UI workflow:
  - admin integration settings with status and retry visibility
- Civic outcome:
  - the system can meet communities where they already coordinate
- Acceptance criteria:
  - [ ] official announcements can fan out to configured channels
  - [ ] activity/resource events can sync to calendar flows
  - [ ] integration failures are visible and retryable
- Validation:
  - [ ] webhook/connector tests
  - [ ] admin integration UX checks

### `story:OCS-P2-006` Verified Identity Tiers

- Problem: sensitive civic actions need stronger identity assurance than basic discussion.
- Scope note:
  - phase-gated because verification carries privacy, compliance, and operational burden
- Focus:
  - pseudonymous participation for discussion
  - verified participation for voting, reporting, or representation

### `story:OCS-P2-007` Auditable Secure Elections

- Problem: formal elections require stronger guarantees than standard voting.
- Scope note:
  - phase-gated until verified identity and threat model are defined

### `story:OCS-P2-008` Participatory Budgeting Allocation

- Problem: budget allocation is valuable but depends on proposal, voting, and project execution foundations.
- Scope note:
  - defer until Waves C and D are stable

### `story:OCS-P2-009` Bounded Civic Reputation

- Problem: reputation can incentivize participation, but can also distort power and create toxicity.
- Scope note:
  - only proceed with bounded weight and explicit anti-gamification rules

## Recommended First Implementation Tranche

1. `OCS-P1-029`
2. `OCS-P1-030`
3. `OCS-P1-031`
4. `OCS-P1-033`
5. `OCS-P1-037`
6. `OCS-P1-038`
7. `OCS-P1-040`
8. `OCS-P1-043`
9. `OCS-P1-046`
10. `OCS-P1-047`

Rationale:
- identity, space, and permission models are prerequisites for most later governance features
- official announcements and issue tracking create immediate user-visible value
- proposals, project boards, and decision ledger complete the trust loop from problem to action
