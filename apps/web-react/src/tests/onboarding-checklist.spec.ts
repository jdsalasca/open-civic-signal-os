import { expect, test } from '@playwright/test';
import { seedAuthenticatedApp } from './helpers/session';
import { mockDashboardRoutes } from './helpers/dashboard';

test.describe('First-action checklist', () => {
  test('shows the next action and navigates to it', async ({ page }) => {
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
    // Registered after the shared helper so it wins.
    await page.route('**/api/onboarding/checklist', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          joinedCommunity: true,
          contributed: false,
          followingContent: false,
          allComplete: false,
        }),
      }));

    await page.goto('/');
    await expect(page.getByTestId('onboarding-checklist')).toBeVisible();
    await expect(page.getByTestId('onboarding-checklist')).toContainText('1 of 3');
    await expect(page.getByTestId('onboarding-step-joinedCommunity')).toBeDisabled();

    await page.getByTestId('onboarding-step-contributed').click();
    await expect(page).toHaveURL(/\/report/);
  });
});
