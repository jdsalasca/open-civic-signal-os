import { expect, test } from '@playwright/test';

/**
 * The backlog must be readable without an account.
 *
 * The API already serves the ranked backlog publicly, so requiring a login to *look at* it asked
 * people to register before they could see whether registering was worth it. These tests assert on
 * the requests, because the failure mode is subtle: a public page that quietly fires an
 * authenticated request renders a login prompt to a stranger instead of a backlog.
 */
const prioritizedPayload = {
  content: [
    {
      id: 'sig-1',
      title: 'Water main break on Calle 12',
      description: 'No supply since Tuesday.',
      category: 'utilities',
      status: 'NEW',
      priorityScore: 313.0,
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
    {
      id: 'sig-2',
      title: 'Streetlight out on the main corridor',
      description: 'Dark for three nights.',
      category: 'infrastructure',
      status: 'IN_PROGRESS',
      priorityScore: 150.0,
      scoreBreakdown: { urgency: 90, impact: 75, affectedPeople: 20, communityVotes: 4 },
      explainabilitySummary: { version: 'v1', topFactors: [], summary: 'High urgency.' },
      latitude: null,
      longitude: null,
      createdAt: '2026-04-02T08:00:00',
      authorUsername: 'vecino',
      commentsCount: 0,
      viewerHasVoted: false,
      communityVotes: 4,
      affectedPeople: 60,
      locationLabel: 'Main corridor',
      sourceChannel: 'WEB_FORM',
      sourceRef: null,
      transformationVersion: 'v1',
    },
  ],
  totalElements: 2,
  totalPages: 1,
  size: 20,
  number: 0,
  first: true,
  last: true,
};

test.describe('Public backlog without an account', () => {
  test('renders the ranked backlog, the formula, and the freshness stamp', async ({ page }) => {
    const requestHeaders: Record<string, string>[] = [];

    await page.route('**/api/signals/prioritized*', async (route) => {
      requestHeaders.push(route.request().headers());
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(prioritizedPayload),
      });
    });

    await page.route('**/api/signals/meta', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          totalSignals: 210,
          unresolvedSignals: 140,
          lastUpdatedAt: '2026-04-01T10:00:00',
          criticalScoreThreshold: 220,
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

    // No stored session at all: a stranger arriving at the URL.
    await page.goto('/backlog');

    await expect(page.getByTestId('public-backlog-list')).toBeVisible({ timeout: 30000 });
    await expect(page.getByTestId('public-backlog-item-1')).toContainText('Water main break');
    await expect(page.getByTestId('public-backlog-item-2')).toContainText('Streetlight out');

    // The two things that make a public ranking auditable rather than a black box.
    await expect(page.getByTestId('public-backlog-formula')).toContainText('Urgency * 30');
    await expect(page.getByTestId('public-backlog-formula')).toContainText('v1');
    await expect(page.getByTestId('public-backlog-freshness')).toBeVisible();

    // Every list exposes why an item is ranked where it is. These are the formula's terms, not
    // raw counts: the breakdown values are already weighted and capped, so labelling them "people
    // affected" would misreport a capped 30 as a headcount.
    await expect(page.getByTestId('public-backlog-why-1')).toContainText('urgency 150');
    await expect(page.getByTestId('public-backlog-why-1')).toContainText('people 30');
    await expect(page.getByTestId('public-backlog-why-1')).toContainText('score terms');

    // The page must not have redirected a stranger to a login screen.
    expect(page.url()).toContain('/backlog');

    // And it must not have sent a credential with the public reads.
    for (const headers of requestHeaders) {
      expect(headers['authorization']).toBeUndefined();
      expect(headers['cookie'] ?? '').not.toContain('accessToken');
    }
  });

  test('shows a sign-in path rather than a dead end', async ({ page }) => {
    await page.route('**/api/signals/prioritized*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(prioritizedPayload),
      });
    });
    await page.route('**/api/signals/meta', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          totalSignals: 1,
          unresolvedSignals: 1,
          lastUpdatedAt: '2026-04-01T10:00:00',
          criticalScoreThreshold: 220,
        }),
      });
    });
    await page.route('**/api/signals/formula', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          version: 'v1',
          formula: 'x',
          effectiveFrom: '2026-03-21',
          weights: [],
          cappedFactors: [],
          changeNote: '',
        }),
      });
    });

    await page.goto('/backlog');
    await expect(page.getByTestId('public-backlog-cta')).toBeVisible({ timeout: 30000 });
  });

  test('an unreachable API shows an unavailable state, not a login prompt', async ({ page }) => {
    await page.route('**/api/signals/prioritized*', async (route) => route.abort('failed'));
    await page.route('**/api/signals/meta', async (route) => route.abort('failed'));
    await page.route('**/api/signals/formula', async (route) => route.abort('failed'));

    await page.goto('/backlog');
    await expect(page.getByTestId('public-backlog-unavailable')).toBeVisible({ timeout: 30000 });
    expect(page.url()).toContain('/backlog');
  });
});
