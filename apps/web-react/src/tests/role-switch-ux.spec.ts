import { expect, test } from '@playwright/test';

test.describe('Role Switch UX (P0)', () => {
  test('Role dropdown is intuitive and keeps selected role across navigation', async ({ page }) => {
    const getActiveCommunityId = async () =>
      page.evaluate(() => {
        const raw = localStorage.getItem('community-storage');
        return raw ? JSON.parse(raw).state?.activeCommunityId : null;
      });

    await page.goto('/login');
    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();
    await page.waitForURL('**/');

    await page.goto('/settings');
    await expect(page.getByTestId('role-switch-dropdown')).toBeVisible();
    await expect.poll(async () => getActiveCommunityId(), { timeout: 10000 }).not.toBeNull();
    const beforeRoleSwitchCommunityId = await getActiveCommunityId();

    await page.getByTestId('role-switch-dropdown').locator('.p-dropdown').click();
    await page.getByText('Citizen', { exact: true }).click();
    await expect(page.getByText('Active role switched to Citizen.')).toBeVisible();

    await page.goto('/moderation');
    await expect(page).toHaveURL(/.*(unauthorized|forbidden)/);

    await page.goto('/report');
    await page.goto('/settings');
    await expect(page.getByTestId('role-switch-dropdown')).toContainText('Citizen');
    expect(await getActiveCommunityId()).toBe(beforeRoleSwitchCommunityId);

    await page.getByTestId('role-switch-dropdown').locator('.p-dropdown').click();
    await page.getByText('Public Servant', { exact: true }).click();
    await expect(page.getByText('Active role switched to Public Servant.')).toBeVisible();
    expect(await getActiveCommunityId()).toBe(beforeRoleSwitchCommunityId);
    await page.goto('/moderation');
    await expect(page).toHaveURL(/.*\/moderation/);
  });
});
