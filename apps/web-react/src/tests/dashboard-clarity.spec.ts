import { expect, test } from '@playwright/test';
import { seedAuthenticatedApp } from './helpers/session';
import { mockDashboardRoutes } from './helpers/dashboard';

/**
 * The dashboard has one dominant primary action and everything else is secondary.
 *
 * This used to log in against a live backend, which made it unrunnable anywhere without a seeded
 * database and nobody had noticed, because no workflow runs this suite. Asserting on layout needs
 * no real API, so it now seeds a session and answers the routes the dashboard fires.
 */
test.describe('Dashboard clarity hierarchy', () => {
  test('shows one dominant primary action and moves the rest into a secondary surface', async ({ page }) => {
    // The shared helper only answers the two routes nobody remembers. These two are the dashboard's
    // own data, and a spec asserting on layout still has to answer them or the request reaches the
    // live backend, 401s, and logs the session out before the hero renders.
    await page.route('**/api/signals/prioritized*', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 0, first: true, last: true }),
      }));
    await page.route('**/api/signals/meta', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ totalSignals: 0, unresolvedSignals: 0, lastUpdatedAt: null, criticalScoreThreshold: 220 }),
      }));
    await page.route('**/api/notifications/recent', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));
    await page.route('**/api/signals/duplicates', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));

    await seedAuthenticatedApp(page);
    await mockDashboardRoutes(page);

    await page.goto('/');
    await expect(page.getByTestId('dashboard-hero')).toBeVisible({ timeout: 30000 });

    await expect(page.getByTestId('dashboard-action-report')).toBeVisible();
    await expect(page.getByTestId('dashboard-primary-guidance')).toBeVisible();
    await expect(page.getByTestId('dashboard-secondary-actions')).toBeVisible();

    const hero = page.getByTestId('dashboard-hero');
    await expect(hero.getByTestId('dashboard-action-report')).toBeVisible();
    await expect(hero.getByTestId('dashboard-action-threads')).toHaveCount(0);
    await expect(hero.getByTestId('dashboard-action-blog')).toHaveCount(0);

    const secondarySurface = page.getByTestId('dashboard-secondary-actions');
    await expect(secondarySurface.getByTestId('dashboard-action-threads')).toBeVisible();
    await expect(secondarySurface.getByTestId('dashboard-action-blog')).toBeVisible();
    await expect(secondarySurface.getByTestId('dashboard-action-mine')).toBeVisible();
  });
});