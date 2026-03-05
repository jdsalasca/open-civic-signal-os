import { expect, test } from '@playwright/test';

test.describe('Global State Integrity (P0)', () => {
  test('Buttons and route navigation must not reset language, theme, or community context', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();
    await page.waitForURL('**/');

    await page.goto('/settings');
    await page.locator('.p-selectbutton >> text=Español').click();
    await page.locator('.p-selectbutton >> text=Modo Claro').click();
    await expect(page.getByText('Configuración del Sistema')).toBeVisible();

    await page.goto('/report');
    await page.getByRole('button', { name: 'Descartar' }).click();
    await expect(page).toHaveURL(/.*\/$/);
    await expect(page.getByTestId('welcome-message')).toBeVisible();

    await page.goto('/settings');
    await expect(page.getByText('Configuración del Sistema')).toBeVisible();
    await expect(page.locator('.p-selectbutton .p-highlight').filter({ hasText: 'Modo Claro' })).toBeVisible();

    const initialCommunityId = await page.evaluate(() => {
      const raw = localStorage.getItem('community-storage');
      return raw ? JSON.parse(raw).state?.activeCommunityId : null;
    });
    await page.goto('/communities/threads');
    await page.goto('/communities/blog');
    await page.goto('/communities/feed');
    await page.goto('/settings');
    const finalCommunityId = await page.evaluate(() => {
      const raw = localStorage.getItem('community-storage');
      return raw ? JSON.parse(raw).state?.activeCommunityId : null;
    });
    expect(finalCommunityId).toBe(initialCommunityId);
  });
});
