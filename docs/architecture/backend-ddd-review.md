# Backend review: from a flat service layer to bounded contexts

Date: 2026-11-16
Scope: `apps/api-java` (Spring Boot 3, Java 21, PostgreSQL/Flyway, H2 test suite).

This review is deliberately conservative: the backend is healthy (475 tests, deterministic
scoring, schema parity), so the goal is to make the domain boundaries explicit without changing
public APIs or data. It records the findings that matter, the target shape, and a staged plan.

## What exists today

```
org.opencivic.signalos
├── domain        entities + enums (Signal, Community, Proposal, ...)
├── repository    Spring Data interfaces
├── service       52 classes, 25 of them Community*
├── web           controllers + DTOs
├── config        security, CORS, OpenAPI
├── exception
└── security
```

The good parts:

- Scoring is backend-owned and deterministic; `PrioritizationFormula` is a domain type, not a
  frontend copy.
- Migrations are versioned and compared against PostgreSQL in CI.
- The ranking and lifecycle rules have tests, including query-count tests for N + 1 regressions.
- Controllers are thin; most rules already live outside the HTTP layer.

The structural problems:

1. **The application layer is flat.** `service/` contains 52 classes and mixes bounded contexts:
   communities, signals, governance, moderation, finance, integrations, digests and identity.
   Finding the owner of a rule means reading filenames.
2. **`CommunityService` has several reasons to change.** Catalog queries, nearest-city lookup,
   join/leave, membership summaries, permission scopes and inactive sweeps live in one class
   (~450 lines). That is the definition of a missing boundary.
3. **The domain is mostly anemic.** Entities carry data and enums; policies such as lifecycle
   transitions and ranking are in services. Some rules are already extracted (`PrioritizationService`,
   `PrioritizationFormula`), which is the pattern to extend.
4. **Cross-context calls are direct service calls.** A digest service calls email, a trust service
   calls communities, and so on. There are no explicit ports, so a context cannot be tested or
   moved without dragging its neighbours.
5. **Read paths still have full scans.** `ExportService` loaded every signal and then re-read each
   one to build its trust packet; the N + 1 is fixed in this round by building the packet from the
   loaded entity. `CommunityService` calls `findAll()` in eight places (acceptable for a small
   catalog, but it is a cache/projection candidate), and the policy-simulation services scan all
   signals.
6. **Nothing enforces the boundaries.** Without an ArchUnit rule, a new service can import any
   package and the flat layout grows back.

## Target shape

Bounded contexts, each with `domain`, `application` and `adapter` packages:

| Context | Owns | Current classes (examples) |
|---|---|---|
| Identity & access | users, roles, Google login, sessions, profiles | `GoogleAuthService`, `UserService`, `ProfileService`, `security/*` |
| Communities & membership | communities, places, memberships, permission policies, invitations | `CommunityService`, `CommunityAccessService`, `CommunityPermissionPolicyService`, `GeocodingService` |
| Signals & prioritization | signals, votes, scoring, duplicates, status timeline, public backlog | `PrioritizationService`, `SignalGeoService`, `SurgeDetectionService`, `ExportService` |
| Governance & decisions | proposals, deliberation, voting, decisions, assemblies, budgets, projects | `CommunityProposal*`, `CommunityDecisionLedgerService`, `CommunityAssemblyService`, `ParticipatoryBudget*` |
| Moderation & trust | reports, sanctions, moderation queue, trust metrics | `CommunityModerationService`, `CommunityTrustMetricsService`, `CommunityPrivacyService` |
| Integrations & digests | email, webhooks, RSS, digest scheduling, open data | `EmailService`, `DigestSchedulerService`, `CommunityIntegration*`, `CommunityOpenData*` |
| Shared kernel | time, ids, pagination, audit metadata | `DataFreshnessService` helpers, `formatStamp` equivalents |

The web layer keeps the same URLs; each controller depends on the application service of its
context. Cross-context work goes through a small port (for example `EmailPort`, `CommunityLookup`)
rather than a concrete service.

## Staged plan

Each step is independently shippable and verifiable.

### Step 1 — package by context, no behavior change

- Create `community/`, `signal/`, `governance/`, `identity/`, `moderation/`, `integration/`,
  `shared/` under `org.opencivic.signalos`, each with `domain`, `application`, `adapter`.
- Move classes mechanically; keep class names and public methods.
- Add ArchUnit tests: web may depend on application, application on domain; a context may not
  import another context's adapters; domain imports no Spring web/JPA repositories.
- Exit criteria: `mvn test` green, OpenAPI unchanged, no SQL diff.

### Step 2 — split `CommunityService`

- `CommunityCatalogService`: public list, nearest, breadcrumbs, inactive sweep.
- `CommunityMembershipService`: join/leave, memberships, role changes, audit.
- `CommunityPermissionService`: permission scopes and policy checks.
- Keep a thin `CommunityFacade` for existing controllers until they are migrated one by one.
- Exit criteria: controller tests unchanged; each new service has a single reason to change.

### Step 3 — read models for the expensive paths

- `ExportService`: stream rows from the repository and write CSV incrementally; the trust packet
  is already built from the loaded entity (done in this round).
- `CommunityService` catalog: one query with a projection or a short-lived cache keyed by the
  catalog version, instead of eight `findAll()` calls.
- Policy simulation: filter by community + date in SQL, not in memory.
- Exit criteria: query-count tests for export and catalog; no `findAll()` in request paths except
  the explicitly bounded catalog.

### Step 4 — domain policies

- Move duplicated lifecycle/status rules into domain value objects (`SignalLifecycle`,
  `MembershipRole`) and let services orchestrate.
- Keep the published formula in `PrioritizationFormula` as the only copy.
- Exit criteria: one definition per concept, with a test that fails if a second copy appears.

## Concrete change in this round

`PrioritizationService.getTrustPacket(Signal)` was added and `ExportService` now uses it. Before,
a CSV export performed one `findAll()` plus one `findById()` per row. The new
`ExportServiceTest` verifies the packet is built from the loaded entity and that the UUID overload
is never called from the export path.

## What this review does not do

- It does not rewrite entities into rich aggregates; that is Step 4 and needs a rule-by-rule
  migration with the existing tests as the safety net.
- It does not change endpoints, tables or migrations.
- It does not move code in this PR; the UI round is kept separate.

## Next step

Open one issue per step with the exit criteria above, starting with Step 1. The package move is
mechanical and reviewable in one PR if imports are updated by the IDE/`rg`; the ArchUnit tests are
what keep it from drifting back.
