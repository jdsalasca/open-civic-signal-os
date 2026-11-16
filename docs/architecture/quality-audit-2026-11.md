# Quality audit — November 2026

Scope: `apps/web-react` and `apps/api-java`, measured with the hard limits used by the teaching
repo (`open-coders-teachers`): 100-character lines, 40-line methods, 4 parameters, cyclomatic
complexity 8, no unused imports or variables, no dead dependencies, styles only in SCSS.

This is a **ratchet**, not a rewrite. The project is 31k lines of Java and a large React app; the
goal is to stop new debt and remove the worst existing pieces without changing behaviour.

## Frontend

Tool: ESLint 8 with the repository's existing TypeScript/React config
(`apps/web-react/.eslintrc.cjs`).

| Measurement | Before | After |
|---|---:|---:|
| ESLint problems | 15 (1 error, 14 warnings) | **0** |
| `tsc`/Vite build | pass | pass |
| Contrast pairs | 102/102 | 102/102 |

Fixed in this round:

- `prefer-const` in `community-trust-metrics.spec.ts`.
- `any` in `axios.ts` (typed refresh queue), `Dashboard.tsx` (error and page event),
  `SignalTable.tsx` (page event) and the mobile navigation spec.
- Stale hook dependencies in `Dashboard`, `Layout`, `Settings`, `CommunityOpenData`,
  `CategoryChart` and `PriorityRadar`. The two chart memos keep `theme` on purpose because it
  invalidates CSS-token reads; both carry a written justification.
- `mergeAffiliationTags` and the membership TTL moved out of the render body.

Gate added:

- `npm run lint:web` (zero warnings).
- Runs in `agent:preflight` whenever frontend files change.
- Runs in the `node-check` CI job.

Still to ratchet:

- 38 `style={{...}}` usages. Most are dynamic (chart height, progress width, map position) or
  PrimeReact props; the static ones can move to SCSS in a later pass.
- The largest components remain `Dashboard`, `Settings`, `CommunityCollaboration` flows; they are
  candidates for extracting view-model hooks rather than giant renders.

## Backend

Tool: a script mirroring the same limits (method length, parameter count, file length,
`findAll()` in request paths, empty/broad catches). Checkstyle's `google_checks.xml` was also run
but produced **16,538** findings dominated by 2-space indentation and Javadoc on every public
member; adopting it wholesale would be a formatting rewrite, not a quality improvement.

Inventory: **427 files**, **31,340 lines**.

| Measurement | Before | After |
|---|---:|---:|
| Methods ≥ 40 lines or > 4 params | 25+ (top 25 listed) | 23 |
| Largest method | 184 lines (`buildTrustMetrics`) | 100 lines (`onboardingStepsFor`) |
| Feed method | 70 lines (`getCommunityFeed`) | split into 4 methods |
| Empty catches | 0 | 0 |
| Broad `catch (Exception/Throwable)` | 19 across 8 files | unchanged |
| `findAll()` in request paths | 8 in `CommunityService` + 7 elsewhere | export N+1 fixed earlier |

Refactored in this round:

- `CommunityTrustMetricsService.buildTrustMetrics` (184 lines) split into
  `loadData`, `activeContributors`, `resolutionHours`, `buildCards` (four small card builders) and
  `buildBreakdowns`, with two private records for the window and the loaded data.
  `CommunityTrustMetricsIT` still passes.
- `CommunityCollaborationService.getCommunityFeed` (70 lines) split into
  `signalFeedItems`, `blogFeedItems` and `threadFeedItems`.
  `CommunityHomeIT` covers the combined order and summaries.

Remaining hotspots (next ratchet):

1. `HelpCenterService.onboardingStepsFor` — 100 lines; mostly a switch over content. Extract one
   method per persona/surface.
2. `InstitutionalActionBridgeService.recordBatchHandoff` — 99 lines; split validation, entity
   creation and response mapping.
3. `BiasDiagnosticService.buildReport` — 91 lines.
4. `AuthController.register` — 89 lines; split city resolution, user creation and welcome email.
5. `CommunityService` — 8 `findAll()` calls on the catalog; replace with a projection or a
   versioned cache.
6. `CommunityOpenDataService` (757 lines), `CommunityCollaborationService` (708),
   `AuthController` (664), `PrioritizationServiceImpl` (655), `SignalController` (614) — split by
   use case when each is touched.

Broad catches: `IntegrationCredentialCipher` (3), `WebhookCommunityIntegrationConnector` (2),
`SignalMergeReviewService` (2), `FormulaChangeProposalService` (2),
`ExplainabilitySnapshotService` (2), `EmailService` (2). Each should either narrow the exception
or carry a written reason; none are empty.

## How to keep it clean

- Frontend: `npm run lint:web` must stay at zero warnings; the CI and preflight already enforce it.
- Backend: adopt a **custom** Checkstyle profile with only high-signal rules (method length,
  parameter count, complexity, unused/redundant imports, empty catches, naming), not
  `google_checks.xml`. Run it first as a report with a baseline, then fail on regressions.
- No new `any`, no new inline static styles, no new `findAll()` in a request path.
- When a method crosses 40 lines, extract the next cohesive step; the two refactors above are the
  pattern.

## Reproduce

```bash
npm run lint:web
npm run build:web
npm run check:contrast
cd apps/api-java && mvn -q -Dtest=CommunityTrustMetricsIT,CommunityHomeIT test
```
