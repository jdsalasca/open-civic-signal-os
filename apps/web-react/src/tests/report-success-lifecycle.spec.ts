import { expect, test } from '@playwright/test';

test.describe.configure({ mode: 'serial' });

test.describe('Report success lifecycle clarity', () => {
  test('successful report explains what happens next and offers follow-up actions', async ({ page }) => {
    const uniqueSuffix = `${Date.now()}-${test.info().project.name}`;

    await page.goto('/login');
    await page.getByTestId('login-username-input').fill('citizen');
    await page.getByTestId('login-password-input').fill('citizen123');
    await page.getByTestId('login-submit-button').click();
    await page.waitForURL('**/');

    await page.goto('/report');
    await page.getByTestId('report-title-input').fill(`Blocked drainage near school entrance ${uniqueSuffix}`);
    await page.getByTestId('report-description-textarea').fill('Water accumulates at the school entrance every afternoon and families have trouble crossing safely after rain.');
    await expect(page.getByTestId('report-submit-button')).toBeEnabled();
    await page.getByTestId('report-submit-button').click();

    await expect(page.getByTestId('report-success-card')).toBeVisible();
    await expect(page.getByTestId('report-success-status')).toContainText('Report submitted');
    await expect(page.getByTestId('report-success-lifecycle-intro')).toContainText('three visible stages');
    await expect(page.getByTestId('report-success-why-ranked')).toBeVisible();
    await expect(page.getByTestId('report-success-go-mine')).toBeVisible();
    await expect(page.getByTestId('report-success-go-detail')).toBeVisible();
    await expect(page.getByTestId('report-success-go-dashboard')).toBeVisible();
  });
});
