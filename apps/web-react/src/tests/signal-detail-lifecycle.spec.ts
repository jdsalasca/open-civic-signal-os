import { expect, test, type Page } from '@playwright/test';

/**
 * The signal detail screen is where a resident follows one case over time, so it is the screen
 * where machine vocabulary hurts most: the header badge, the timeline badge and the transition
 * sentence all published the raw lifecycle enum, and the timeline timestamp changed shape with
 * the reader's browser locale.
 *
 * Everything here is mocked. AuthGuard only reads the persisted store, so seeding localStorage is
 * enough to reach the route without a backend or a seeded account.
 */
const signalId = '22222222-2222-2222-2222-222222222222';

// The wire sends a Java LocalDateTime, i.e. no offset and no seconds.
const createdAt = '2026-04-01T10:00:00';

async function seedSession(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem(
      'auth-storage',
      JSON.stringify({
        state: {
          accessToken: 'test-token',
          userName: 'liaison',
          activeRole: 'PUBLIC_SERVANT',
          rawRoles: ['PUBLIC_SERVANT'],
          isLoggedIn: true,
          isHydrated: true,
        },
        version: 0,
      }),
    );
  });
}

async function mockDetail(page: Page) {
  // The axios response interceptor calls logout() on a 401 that survives the refresh attempt,
  // which would wipe the seeded store and bounce the test back to /login. Answering auth/me keeps
  // the session this spec fabricated, so the run needs no backend and no seeded account.
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        username: 'liaison',
        role: 'PUBLIC_SERVANT',
        interfaceMode: 'ADVANCED',
      }),
    });
  });

  // communities/my is fired by the shared Layout on every authenticated page, and comments by the
  // engagement panel. Left unmocked they reach the live backend, whose 401 survives the refresh
  // attempt and trips logout(), wiping the seeded store before the view ever renders.
  await page.route('**/api/communities/my', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) });
  });

  await page.route(`**/api/signals/${signalId}/comments`, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) });
  });

  await page.route(`**/api/signals/${signalId}/history`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: '44444444-4444-4444-4444-444444444444',
          signalId,
          eventType: 'STATUS_CHANGED',
          statusFrom: 'NEW',
          statusTo: 'IN_PROGRESS',
          changedBy: 'staff',
          assignedToUsername: null,
          reason: 'Inspection started this morning',
          createdAt,
        },
      ]),
    });
  });

  await page.route(`**/api/signals/${signalId}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: signalId,
        title: 'Broken pedestrian bridge railing',
        description: 'The railing is loose and children cross here every morning.',
        category: 'infrastructure',
        status: 'IN_PROGRESS',
        assignedToUsername: 'liaison',
        locationLabel: 'Pedestrian bridge by the sports field',
        evidenceUrls: ['https://example.com/bridge-1.jpg'],
        priorityScore: 198,
        scoreBreakdown: { urgency: 120, impact: 60, affectedPeople: 8, communityVotes: 10 },
        communityVotes: 12,
        reactions: {},
        explainabilitySummary: {
          version: 'v1',
          topFactors: [
            { key: 'urgency', contribution: 120 },
            { key: 'impact', contribution: 60 },
          ],
          summary: 'Top drivers: urgency (120.0), impact (60.0)',
        },
      }),
    });
  });

  await page.route('**/api/signals/formula', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        version: 'v1',
        formula: '(Urgency * 30) + (Impact * 25) + min(People/10, 30) + min(Votes/5, 15)',
        effectiveFrom: '2026-03-21',
        weights: [],
        cappedFactors: ['affectedPeople', 'communityVotes'],
        changeNote: 'Initial published formula.',
      }),
    });
  });
}

test.describe('Signal detail speaks plain language', () => {
  test.beforeEach(async ({ page }) => {
    await seedSession(page);
    await mockDetail(page);
  });

  test('the header badge spells out the lifecycle state', async ({ page }) => {
    await page.goto(`/signal/${signalId}`);
    await expect(page.getByTestId('signal-detail-location-label')).toBeVisible({ timeout: 30000 });

    await expect(page.getByTestId('signal-detail-status-badge')).toHaveText('In progress');

    // IN_PROGRESS is an identifier for the API, not something a resident can act on.
    await expect(page.getByText('IN_PROGRESS')).toHaveCount(0);
  });

  test('the timeline transition reads as two states, not two identifiers', async ({ page }) => {
    await page.goto(`/signal/${signalId}`);
    await expect(page.getByTestId('signal-timeline-entry-0')).toBeVisible({ timeout: 30000 });

    await expect(page.getByTestId('signal-timeline-entry-0')).toContainText('From New to In progress');
    await expect(page.getByTestId('signal-timeline-entry-0')).not.toContainText('IN_PROGRESS');
  });
});

test.describe('Signal detail timestamps do not depend on the reader browser locale', () => {
  // Same defect as the public backlog freshness stamp: toLocaleString with no argument renders
  // from the browser's default locale, so the same audit trail reads "4/1/2026, 10:00:00 AM" to one
  // resident and "1.4.2026, 10:00:00" to the next.
  test.use({ locale: 'de-DE' });

  test('renders the audit trail the same way under a non-English locale', async ({ page }) => {
    await seedSession(page);
    await mockDetail(page);

    await page.goto(`/signal/${signalId}`);
    await expect(page.getByTestId('signal-timeline-entry-0')).toBeVisible({ timeout: 30000 });

    await expect(page.getByTestId('signal-timeline-entry-0')).toContainText('2026-04-01 10:00');
    await expect(page.getByTestId('signal-timeline-entry-0')).not.toContainText('a. m.');
  });
});