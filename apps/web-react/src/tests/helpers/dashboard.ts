import type { Page } from '@playwright/test';

/**
 * The two routes the dashboard fires that no spec remembers to mock.
 *
 * Both were found the slow way: the spec rendered, `dashboard-hero` appeared, and then the run
 * silently landed on /login. `signals/aging` and `help-center` reached the live backend, returned
 * 401, the refresh attempt returned 403, and axios called `logout()` - which wipes the seeded
 * session. Every assertion after that point fails on a page nobody was looking at.
 *
 * The lesson generalises past these two: a spec that mocks the routes it knows about is one unmocked
 * request away from being useless, and the symptom points at the view rather than at the gap. Any
 * spec that renders the dashboard needs these.
 *
 * Specs that assert on aging or help content should register their own route for it. Playwright
 * checks the most recently added route first, so a later registration wins over these.
 */
const json = (body: unknown) => ({
  status: 200,
  contentType: 'application/json',
  body: JSON.stringify(body),
});

/**
 * Answer the contextual help panel.
 *
 * Split out from `mockDashboardRoutes` because of a registration-order trap: Playwright checks the
 * most recently added route first, so calling the combined helper after a spec registered its own
 * aging payload would silently override it. A spec that asserts on aging content needs this one
 * alone and keeps its own route.
 */
export async function mockHelpCenter(page: Page) {
  await page.route('**/api/help-center*', (route) =>
    route.fulfill(
      json({
        persona: 'citizen',
        language: 'en',
        surface: 'DASHBOARD',
        query: null,
        generatedAt: '2026-04-01T12:00:00',
        completedStepKeys: [],
        dismissedGuideKeys: [],
        onboardingSteps: [],
        guides: [],
      }),
    ));
}

/** The two routes the dashboard fires that no spec remembers to mock. */
export async function mockDashboardRoutes(page: Page) {
  // The aging payload has to satisfy SignalAging exactly. A missing `atRiskSignals` crashes the
  // dashboard at `aging.atRiskSignals.length`, and the crash is not a blank screen: React unmounts
  // the tree, the boundary retries, it crashes again, and the dashboard re-requests aging about
  // twelve times a second. Both required arrays are therefore present here rather than omitted.
  await page.route('**/api/signals/aging*', (route) =>
    route.fulfill(
      json({
        communityId: null,
        generatedAt: '2026-04-01T12:00:00',
        slaTargetDays: 30,
        unresolvedCount: 0,
        atRiskCount: 0,
        breachedCount: 0,
        medianAgeDays: 0,
        ageBuckets: [],
        atRiskSignals: [],
        trend: [],
      }),
    ));

  await mockHelpCenter(page);
}