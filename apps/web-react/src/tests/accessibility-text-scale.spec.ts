import { expect, test } from '@playwright/test';
import { seedAuthenticatedApp } from './helpers/session';
import { mockDashboardRoutes } from './helpers/dashboard';

test.describe('Comfortable reading', () => {
  test('large text scales the root and survives a reload', async ({ page }) => {
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

    await seedAuthenticatedApp(page, 'CITIZEN');
    await mockDashboardRoutes(page);

    await page.goto('/');
    await expect(page.getByTestId('dashboard-hero')).toBeVisible();

    const baseSize = await page.evaluate(() => getComputedStyle(document.documentElement).fontSize);
    expect(baseSize).toBe('16px');

    await page.getByTestId('theme-wheel-trigger').click();
    await page.getByTestId('text-scale-large').click();
    await expect
      .poll(() => page.evaluate(() => getComputedStyle(document.documentElement).fontSize))
      .toBe('18px');

    await page.reload();
    await expect(page.getByTestId('dashboard-hero')).toBeVisible();
    await expect
      .poll(() => page.evaluate(() => getComputedStyle(document.documentElement).fontSize))
      .toBe('18px');
    await expect
      .poll(() => page.evaluate(() => document.documentElement.dataset.textScale))
      .toBe('large');
  });
});
