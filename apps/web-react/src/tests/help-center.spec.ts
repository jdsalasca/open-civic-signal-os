import { expect, test } from '@playwright/test';

async function login(page: import('@playwright/test').Page, username: string, password: string) {
  await page.goto('/login');
  await page.getByTestId('login-username-input').fill(username);
  await page.getByTestId('login-password-input').fill(password);
  await page.getByTestId('login-submit-button').click();
  await page.waitForURL('**/');
}

test.describe('Help center onboarding and contextual guidance', () => {
  test('dashboard contextual help opens the full help center guide', async ({ page }) => {
    await login(page, 'citizen', 'citizen123');

    await expect(page.getByTestId('dashboard-contextual-help')).toBeVisible();
    await expect(page.getByTestId('dashboard-contextual-help')).toContainText('Start with one guided action');

    await page.getByTestId('dashboard-contextual-help').getByRole('button', { name: 'Open full guide' }).click();

    await expect(page).toHaveURL(/\/help\?surface=DASHBOARD&guide=dashboard-start-here/);
    await expect(page.getByTestId('help-center-onboarding-card')).toBeVisible();
    await expect(page.getByTestId('help-guide-dashboard-start-here')).toBeVisible();
  });

  test('help center supports surface filters and searchable guide retrieval', async ({ page }) => {
    await login(page, 'citizen', 'citizen123');

    await page.goto('/help');
    await expect(page.getByTestId('help-center-guides-card')).toBeVisible();

    await page.getByTestId('help-center-search').fill('evidence');
    await expect(page.getByTestId('help-guide-report-good-evidence')).toBeVisible();

    await page.getByTestId('help-center-surface-filter').locator('.p-dropdown').click();
    await page.getByText('Report', { exact: true }).click();

    await expect(page).toHaveURL(/surface=REPORT/);
    await expect(page.getByTestId('help-guide-report-good-evidence')).toBeVisible();
  });
});
