import { expect, test } from '@playwright/test';

test.describe('Role Switch UX (P0)', () => {
  test('Role dropdown is intuitive and keeps selected role across navigation', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();
    await page.waitForURL('**/');

    await page.goto('/settings');
    await expect(page.getByTestId('role-switch-dropdown')).toBeVisible();

    await page.getByTestId('role-switch-dropdown').locator('.p-dropdown').click();
    await page.getByText('Citizen', { exact: true }).click();
    await expect(page.getByText('Active role switched to Citizen.')).toBeVisible();

    await page.goto('/moderation');
    await expect(page).toHaveURL(/.*(unauthorized|forbidden)/);

    await page.goto('/report');
    await page.goto('/settings');
    await expect(page.getByTestId('role-switch-dropdown')).toContainText('Citizen');

    await page.getByTestId('role-switch-dropdown').locator('.p-dropdown').click();
    await page.getByText('Public Servant', { exact: true }).click();
    await expect(page.getByText('Active role switched to Public Servant.')).toBeVisible();
    await page.goto('/moderation');
    await expect(page).toHaveURL(/.*\/moderation/);
  });
});
