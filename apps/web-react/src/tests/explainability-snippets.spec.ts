import { test, expect } from '@playwright/test';

test.describe('Explainability snippets in dashboard list', () => {
  test('shows list-level explainability and keeps detail explainability visible', async ({ page }) => {
    const signalId = '11111111-1111-1111-1111-111111111111';
    const mockedSignal = {
      id: signalId,
      title: 'Flooded avenue after rain',
      description: 'Drainage collapse blocks mobility and damages homes.',
      category: 'infrastructure',
      status: 'NEW',
      priorityScore: 241,
      scoreBreakdown: { urgency: 150, impact: 100, affectedPeople: 7, communityVotes: 4 },
      communityVotes: 20,
      reactions: {},
      explainabilitySummary: {
        version: 'v1',
        topFactors: [
          { key: 'urgency', contribution: 150 },
          { key: 'impact', contribution: 100 }
        ],
        summary: 'Top drivers: urgency (150.0), impact (100.0)'
      }
    };

    await page.route('**/api/signals/prioritized**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [mockedSignal],
          totalElements: 1,
          totalPages: 1,
          size: 10,
          number: 0,
          first: true,
          last: true
        })
      });
    });

    await page.route('**/api/signals/meta', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          totalSignals: 1,
          unresolvedSignals: 1,
          lastUpdatedAt: new Date().toISOString()
        })
      });
    });

    await page.route(`**/api/signals/${signalId}`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockedSignal)
      });
    });

    await page.route(`**/api/signals/${signalId}/history`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([])
      });
    });

    await page.goto('/login');
    await page.waitForSelector('[data-testid="login-card"]');
    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();

    await page.waitForSelector('[data-testid="auth-loading"]', { state: 'detached' }).catch(() => {});
    await expect(page.getByTestId('signals-datatable')).toBeVisible({ timeout: 30000 });

    const explainabilitySnippet = page.locator('[data-testid^="signal-explainability-"]').first();
    await expect(explainabilitySnippet).toBeVisible();

    await page.locator('.p-datatable-tbody tr').first().click();
    await expect(page).toHaveURL(/\/signal\//);
    await expect(page.getByTestId('signal-detail-why-ranked')).toBeVisible();
  });
});
