import { expect, test } from '@playwright/test';
import { seedAuthenticatedApp } from './helpers/session';
import { mockDashboardRoutes } from './helpers/dashboard';

/**
 * A dashboard filter that only changes the highlight is a filter that lies.
 *
 * The CRITICAL filter used to run in the browser over the rows already fetched, so the count
 * shown to a resident was "how many of the twenty rows I happen to have are critical" rather
 * than "how many critical signals exist". These tests assert on the requests the page actually
 * makes, because that is the only thing that distinguishes a real filter from a decoration.
 */
const json = (body: unknown) => ({
  status: 200,
  contentType: 'application/json',
  body: JSON.stringify(body),
});

test.describe('Dashboard filters change the query', () => {
  test('CRITICAL filter must reach the API as minScore, not just repaint rows', async ({ page }) => {
    const prioritizedQueries: string[] = [];

    await page.route('**/api/signals/prioritized*', async (route) => {
      prioritizedQueries.push(new URL(route.request().url()).search);
      await route.fulfill(
        json({
          content: [
            {
              id: 'sig-critical-1',
              title: 'Water main break on Calle 12',
              description: 'No supply since Tuesday morning.',
              category: 'utilities',
              status: 'NEW',
              priorityScore: 313,
              scoreBreakdown: { urgency: 150, impact: 125, affectedPeople: 30, communityVotes: 8 },
              explainabilitySummary: { version: 'v1', topFactors: [], summary: 'Urgency and impact are high.' },
              latitude: null,
              longitude: null,
              createdAt: '2026-04-01T08:00:00',
              authorUsername: 'vecina',
              commentsCount: 0,
              viewerHasVoted: false,
              communityVotes: 8,
              affectedPeople: 900,
              locationLabel: 'Calle 12',
              sourceChannel: 'WEB_FORM',
              sourceRef: null,
              transformationVersion: 'v1',
            },
          ],
          totalElements: 47,
          totalPages: 3,
          size: 20,
          number: 0,
          first: true,
          last: false,
        }),
      );
    });

    await page.route('**/api/signals/meta', async (route) => {
      await route.fulfill(
        json({ totalSignals: 210, unresolvedSignals: 140, lastUpdatedAt: '2026-04-01T10:00:00', criticalScoreThreshold: 220 }),
      );
    });

    await page.route('**/api/notifications/recent', async (route) => route.fulfill(json([])));

    // The dashboard fetches this whenever the active role is staff.
    await page.route('**/api/signals/duplicates', async (route) => route.fulfill(json([])));

    await seedAuthenticatedApp(page);
    await mockDashboardRoutes(page);
    await page.goto('/');
    await expect(page.getByTestId('dashboard-hero')).toBeVisible({ timeout: 30000 });

    // The unfiltered page must not send a threshold.
    expect(
      prioritizedQueries[0],
      `first request was "${prioritizedQueries[0]}" of ${JSON.stringify(prioritizedQueries)}`,
    ).not.toContain('minScore');

    // Let the chip settle before clicking: the row re-renders for a short while after the first
    // fetch lands, and Playwright auto-waits for the click to become actionable anyway.
    await page.waitForTimeout(2500);
    await page.getByTestId('dashboard-filter-critical').click();

    // Wait for a request that actually carries the threshold, rather than sleeping and hoping.
    await expect
      .poll(() => prioritizedQueries.some((query) => query.includes('minScore=')), {
        timeout: 20000,
      })
      .toBe(true);

    const filtered = prioritizedQueries.find((query) => query.includes('minScore=')) ?? '';
    // The threshold must be the one the backend published, not a client constant.
    expect(filtered).toContain('minScore=220');

    // And it must be a distinct request, not a client-side repaint of the same rows.
    expect(filtered).toContain('page=0');
  });

  test('status filter must still reach the API as status', async ({ page }) => {
    const queries: string[] = [];

    await page.route('**/api/signals/prioritized*', async (route) => {
      queries.push(new URL(route.request().url()).search);
      await route.fulfill(
        json({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 0, first: true, last: true }),
      );
    });

    await page.route('**/api/signals/meta', async (route) => {
      await route.fulfill(
        json({ totalSignals: 210, unresolvedSignals: 140, lastUpdatedAt: '2026-04-01T10:00:00', criticalScoreThreshold: 220 }),
      );
    });

    await page.route('**/api/notifications/recent', async (route) => route.fulfill(json([])));

    // The dashboard fetches this whenever the active role is staff, same as in the first test.
    await page.route('**/api/signals/duplicates', async (route) => route.fulfill(json([])));

    await seedAuthenticatedApp(page);
    await mockDashboardRoutes(page);
    await page.goto('/');
    await expect(page.getByTestId('dashboard-hero')).toBeVisible({ timeout: 30000 });

    await page.getByTestId('dashboard-filter-in_progress').click();

    await expect.poll(() => queries.some((query) => query.includes('status=IN_PROGRESS')), {
      timeout: 15000,
    }).toBe(true);
  });
});