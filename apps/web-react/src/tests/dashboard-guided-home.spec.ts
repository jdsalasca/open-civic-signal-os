import { expect, test } from '@playwright/test';

async function login(page: import('@playwright/test').Page, username: string, password: string) {
  await page.goto('/login');
  await page.getByTestId('login-username-input').fill(username);
  await page.getByTestId('login-password-input').fill(password);
  await page.getByTestId('login-submit-button').click();
  await page.waitForURL('**/');
  await expect(page.getByTestId('dashboard-hero')).toBeVisible({ timeout: 30000 });
}

test.describe('Dashboard guided home by role', () => {
  test('citizen sees report, support, and follow guidance', async ({ page }) => {
    await login(page, 'citizen', 'citizen123');

    await expect(page.getByTestId('dashboard-guided-persona')).toContainText('Citizen path');
    await expect(page.getByTestId('dashboard-active-community-pill')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Report one local issue clearly' })).toBeVisible();
    await expect(page.getByTestId('dashboard-action-report')).toContainText('Report a local issue');
    await expect(page.getByTestId('dashboard-community-hub-card')).toContainText('Your place in the community');
    await expect(page.getByTestId('dashboard-community-action-blog')).toContainText('Official updates');
    await expect(page.getByTestId('dashboard-community-action-threads')).toContainText('Community talks');
  });

  test('public servant sees publish, review, and resolve guidance', async ({ page }) => {
    await login(page, 'admin', 'admin12345');

    await expect(page.getByTestId('dashboard-guided-persona')).toContainText('Public servant path');
    await expect(page.getByRole('heading', { name: 'Move one priority forward today' })).toBeVisible();
    await expect(page.getByTestId('dashboard-action-report')).toContainText('Publish a progress update');
    await expect(page.getByTestId('dashboard-community-hub-card')).toContainText('Your place in the community');
    await expect(page.getByTestId('dashboard-community-open-hub')).toContainText('Open communities hub');
  });

  test('community moderator context sees moderation and community-health guidance', async ({ page }) => {
    await login(page, 'admin', 'admin12345');

    await page.goto('/settings');
    await page.getByTestId('role-switch-dropdown').locator('.p-dropdown').click();
    await page.getByText('Citizen', { exact: true }).click();
    await expect(page.getByText('Active role switched to Citizen.')).toBeVisible();

    await page.goto('/');
    await expect(page.getByTestId('dashboard-guided-persona')).toContainText('Moderator path');
    await expect(page.getByRole('heading', { name: 'Keep community conversations usable' })).toBeVisible();
    await expect(page.getByTestId('dashboard-action-report')).toContainText('Review community dialogues');
    await expect(page.getByTestId('dashboard-guided-home-description')).toContainText('Los rosales');
    await expect(page.getByTestId('dashboard-community-summary')).toContainText('Los Rosales');
  });
});
