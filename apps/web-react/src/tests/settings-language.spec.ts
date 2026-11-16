import { expect, test } from '@playwright/test';
import { seedAuthenticatedApp } from './helpers/session';
import { mockDashboardRoutes } from './helpers/dashboard';

// The exact case reported from Colombia: an English browser installed on a Spanish-speaking
// machine. The timezone is the signal that the person is in a Spanish-speaking place, so the app
// must open in Spanish; an explicit choice in the wheel must win and persist.
test.use({ locale: 'en-US', timezoneId: 'America/Bogota' });

test.describe('Language follows the place, not the installer', () => {
  test('opens in Spanish in a Spanish-speaking timezone and remembers an explicit switch', async ({ page }) => {
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
    await expect(page.locator('html')).toHaveAttribute('lang', 'es');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Lo que pasa en');

    await page.getByTestId('theme-wheel-trigger').click();
    await page.getByTestId('language-wheel-en').click();
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('What is happening in');

    await page.reload();
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('What is happening in');
  });
});
