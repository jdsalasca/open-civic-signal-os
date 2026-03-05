import { expect, test } from '@playwright/test';

test('signal detail shows assignee and timeline events', async ({ page }) => {
  const signalId = '22222222-2222-2222-2222-222222222222';

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
            { key: 'impact', contribution: 60 }
          ],
          summary: 'Top drivers: urgency (120.0), impact (60.0)'
        }
      })
    });
  });

  await page.route(`**/api/signals/${signalId}/history`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: '33333333-3333-3333-3333-333333333333',
          signalId,
          eventType: 'ASSIGNED',
          statusFrom: 'NEW',
          statusTo: 'NEW',
          changedBy: 'staff',
          assignedToUsername: 'liaison',
          reason: 'Mobility liaison will coordinate the repair',
          createdAt: new Date().toISOString()
        },
        {
          id: '44444444-4444-4444-4444-444444444444',
          signalId,
          eventType: 'STATUS_CHANGED',
          statusFrom: 'NEW',
          statusTo: 'IN_PROGRESS',
          changedBy: 'staff',
          assignedToUsername: null,
          reason: 'Inspection started this morning',
          createdAt: new Date().toISOString()
        }
      ])
    });
  });

  await page.goto('/login');
  await page.getByTestId('login-username-input').fill('admin');
  await page.getByTestId('login-password-input').fill('admin12345');
  await page.getByTestId('login-submit-button').click();
  await page.waitForURL('**/');

  await page.goto(`/signals/${signalId}`);
  await expect(page.getByTestId('signal-detail-assignment-card')).toBeVisible();
  await expect(page.getByTestId('signal-assignee-current')).toContainText('liaison');
  await expect(page.getByTestId('signal-detail-timeline')).toBeVisible();
  await expect(page.getByTestId('signal-timeline-entry-0')).toContainText('liaison');
  await expect(page.getByTestId('signal-timeline-entry-1')).toContainText('Inspection started this morning');
});
