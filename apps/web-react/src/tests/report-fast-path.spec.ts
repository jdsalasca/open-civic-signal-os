import { expect, test } from '@playwright/test';
import { seedAuthenticatedApp } from './helpers/session';

const json = (body: unknown) => ({
  status: 200,
  contentType: 'application/json',
  body: JSON.stringify(body),
});

test.describe('Report fast path', () => {
  test('sends from step two with the suggested priority', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN');
    await page.route('**/api/help-center*', (route) =>
      route.fulfill(
        json({
          persona: 'citizen',
          language: 'en',
          surface: 'REPORT',
          query: null,
          generatedAt: '2026-11-16T12:00:00',
          completedStepKeys: [],
          dismissedGuideKeys: [],
          onboardingSteps: [],
          guides: [],
        }),
      ));
    await page.route('**/api/signals', (route) => {
      if (route.request().method() !== 'POST') {
        return route.continue();
      }
      return route.fulfill(
        json({
          id: 'signal-fast-path',
          title: 'Broken streetlight on 5th',
          priorityScore: 180,
          explainabilitySummary: { summary: 'Urgency drives this priority.', topFactors: [] },
        }),
      );
    });

    await page.goto('/report');
    await page.getByTestId('report-title-input').fill('Broken streetlight on 5th');
    await page.getByTestId('report-location-input').fill('5th avenue, block 2');
    await page.getByTestId('report-next-button').click();

    await page.getByTestId('report-description-textarea').fill(
      'The whole block is dark at night and people cannot see the sidewalk.',
    );
    await page.getByTestId('report-submit-with-suggested').click();

    await expect(page.getByTestId('report-success-card')).toBeVisible();
    await expect(page.getByTestId('report-success-title')).toContainText('Broken streetlight on 5th');
  });
});
