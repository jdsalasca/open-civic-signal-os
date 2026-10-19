import { expect, test, type Page } from '@playwright/test';
import { seedAuthenticatedApp } from './helpers/session';
import { mockHelpCenter } from './helpers/dashboard';

/**
 * Field mode is a promise about bytes, so the test asserts on the network, not on the DOM.
 *
 * Hiding a panel after fetching it still pays for the bytes, which is the entire cost being
 * avoided. A test that only checked the panel was absent would pass for an implementation that
 * fetched everything and then hid it.
 */const prioritizedPayload = {
  content: [
    {
      id: 'sig-1',
      title: 'Water main break',
      description: 'No supply since Tuesday.',
      category: 'utilities',
      status: 'NEW',
      priorityScore: 313.0,
      scoreBreakdown: { urgency: 150, impact: 125, affectedPeople: 30, communityVotes: 8 },
      explainabilitySummary: { version: 'v1', topFactors: [], summary: 'High urgency.' },
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
  totalElements: 1,
  totalPages: 1,
  size: 10,
  number: 0,
  first: true,
  last: true,
};

async function stubDashboard(page: Page, requested: string[]) {
  await page.route('**/api/signals/prioritized*', async (route) => {
    requested.push('prioritized');
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(prioritizedPayload) });
  });
  await page.route('**/api/signals/meta', async (route) => {
    requested.push('meta');
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
  await page.route('**/api/signals/duplicates', async (route) => {
    requested.push('duplicates');
    await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
  });
  await page.route('**/api/notifications/recent', async (route) => {
    requested.push('notifications');
    await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
  });
  await page.route('**/api/signals/aging', async (route) => {
    requested.push('aging');
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        communityId: null,
        generatedAt: '2026-04-01T12:00:00',
        slaTargetDays: 30,
        totalOpen: 0,
        breachedSla: 0,
        medianAgeDays: 0,
        buckets: [],
        items: [],
        trend: [],
      }),
    });
  });
}

test.describe('Field data mode', () => {
  test('full mode requests every panel', async ({ page }) => {
    const requested: string[] = [];
    await seedAuthenticatedApp(page, 'PUBLIC_SERVANT', { dataMode: 'full' });
    await mockHelpCenter(page);
    await page.route("**/api/signals/export/csv*", (route) => route.fulfill({ status: 200, contentType: "text/csv", body: "id,title" }));
    await page.route("**/api/auth/profile/me*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ username: "liaison", email: "liaison@example.com", displayName: "liaison", bio: "" }) }));
    await page.route("**/api/auth/privacy/access-logs*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: "[]" }));
    await page.route("**/api/communities/*/privacy*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ communityId: "11111111-2222-3333-4444-555555555555", openDataPolicy: "RESTRICTED" }) }));
    await stubDashboard(page, requested);

    await page.goto('/');
    await expect(page.getByTestId('dashboard-hero')).toBeVisible({ timeout: 30000 });
    await expect.poll(() => requested.includes('aging'), { timeout: 15000 }).toBe(true);

    expect(requested).toContain('duplicates');
    expect(requested).toContain('notifications');
    expect(requested).toContain('aging');
  });

  test('field mode does not request the optional panels at all', async ({ page }) => {
    const requested: string[] = [];
    await seedAuthenticatedApp(page, 'PUBLIC_SERVANT', { dataMode: 'field' });
    await mockHelpCenter(page);
    await page.route("**/api/signals/export/csv*", (route) => route.fulfill({ status: 200, contentType: "text/csv", body: "id,title" }));
    await page.route("**/api/auth/profile/me*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ username: "liaison", email: "liaison@example.com", displayName: "liaison", bio: "" }) }));
    await page.route("**/api/auth/privacy/access-logs*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: "[]" }));
    await page.route("**/api/communities/*/privacy*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ communityId: "11111111-2222-3333-4444-555555555555", openDataPolicy: "RESTRICTED" }) }));
    await stubDashboard(page, requested);

    await page.goto('/');
    await expect(page.getByTestId('dashboard-hero')).toBeVisible({ timeout: 30000 });
    // Give the optional fetches a chance to happen if they were going to.
    await page.waitForTimeout(2500);

    // The core data still loads.
    expect(requested).toContain('prioritized');
    expect(requested).toContain('meta');

    // And the optional ones are never requested. Hiding them after fetching would still pay.
    expect(requested).not.toContain('duplicates');
    expect(requested).not.toContain('notifications');
    expect(requested).not.toContain('aging');
  });

  test('field mode asks for fewer rows', async ({ page }) => {
    const sizes: string[] = [];
    await seedAuthenticatedApp(page, 'PUBLIC_SERVANT', { dataMode: 'field' });
    await mockHelpCenter(page);
    await page.route("**/api/signals/export/csv*", (route) => route.fulfill({ status: 200, contentType: "text/csv", body: "id,title" }));
    await page.route("**/api/auth/profile/me*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ username: "liaison", email: "liaison@example.com", displayName: "liaison", bio: "" }) }));
    await page.route("**/api/auth/privacy/access-logs*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: "[]" }));
    await page.route("**/api/communities/*/privacy*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ communityId: "11111111-2222-3333-4444-555555555555", openDataPolicy: "RESTRICTED" }) }));
    await page.route('**/api/signals/prioritized*', async (route) => {
      sizes.push(new URL(route.request().url()).searchParams.get('size') ?? '');
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(prioritizedPayload) });
    });
    await page.route('**/api/signals/meta', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          totalSignals: 1, unresolvedSignals: 1,
          lastUpdatedAt: '2026-04-01T10:00:00', criticalScoreThreshold: 220,
        }),
      });
    });

    await page.goto('/');
    await expect(page.getByTestId('dashboard-hero')).toBeVisible({ timeout: 30000 });
    await expect.poll(() => sizes.length > 0, { timeout: 15000 }).toBe(true);

    // Each row is bytes, so field mode asks for half as many.
    expect(sizes[0]).toBe('5');
  });

  test('the setting says exactly what field mode stops fetching', async ({ page }) => {
    await seedAuthenticatedApp(page, 'PUBLIC_SERVANT', { dataMode: 'field' });
    await mockHelpCenter(page);
    await page.route("**/api/signals/export/csv*", (route) => route.fulfill({ status: 200, contentType: "text/csv", body: "id,title" }));
    await page.route("**/api/auth/profile/me*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ username: "liaison", email: "liaison@example.com", displayName: "liaison", bio: "" }) }));
    await page.route("**/api/auth/privacy/access-logs*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: "[]" }));
    await page.route("**/api/communities/*/privacy*", (route) => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ communityId: "11111111-2222-3333-4444-555555555555", openDataPolicy: "RESTRICTED" }) }));
    await page.route('**/api/auth/me', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
    });

    await page.goto('/settings');
    await expect(page.getByTestId('data-mode-select')).toBeVisible({ timeout: 30000 });

    // A mode that silently dropped panels would be a mystery rather than a setting.
    const skips = page.getByTestId('data-mode-skips');
    await expect(skips).toBeVisible();
    await expect(skips).toContainText('signals/duplicates');
    await expect(skips).toContainText('signals/aging');
  });
});
