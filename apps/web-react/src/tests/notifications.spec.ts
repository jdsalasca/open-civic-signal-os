import { expect, test } from '@playwright/test';
import { seedAuthenticatedApp } from './helpers/session';
import { mockDashboardRoutes } from './helpers/dashboard';

const now = new Date().toISOString();

test.describe('Personal notifications', () => {
  test('shows the unread count, opens the inbox and marks everything read', async ({ page }) => {
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
    await seedAuthenticatedApp(page, 'CITIZEN');
    await mockDashboardRoutes(page);

    // Register after the shared bootstrap helper so these payloads win (Playwright checks the
    // most recently added route first).
    await page.route('**/api/notifications/unread-count', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ unread: 2 }) }));
    await page.route('**/api/notifications/inbox', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'n1',
            type: 'THREAD_REPLY',
            actorName: 'Ana',
            targetType: 'THREAD',
            targetId: 't1',
            targetTitle: 'Parque central',
            createdAt: now,
            readAt: null,
          },
          {
            id: 'n2',
            type: 'SIGNAL_SUPPORT',
            actorName: 'Luis',
            targetType: 'SIGNAL',
            targetId: 's1',
            targetTitle: 'Luz de la calle 3',
            createdAt: now,
            readAt: null,
          },
        ]),
      }));
    await page.route('**/api/notifications/read-all', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '{}' }));

    await page.goto('/');
    await expect(page.getByTestId('dashboard-hero')).toBeVisible();

    await expect(page.getByTestId('notification-badge')).toHaveText('2');
    await page.getByTestId('notification-bell').click();
    await expect(page.getByText(/Ana replied in "Parque central"/)).toBeVisible();
    await expect(page.getByText(/Luis supported "Luz de la calle 3"/)).toBeVisible();

    await page.getByTestId('notification-mark-all').click();
    await expect(page.getByTestId('notification-badge')).toHaveCount(0);
  });
});
