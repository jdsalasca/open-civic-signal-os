import { expect, test } from '@playwright/test';

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login');
  await page.getByTestId('login-username-input').fill('admin');
  await page.getByTestId('login-password-input').fill('admin12345');
  await page.getByTestId('login-submit-button').click();
  await page.waitForURL('**/');
}

test.describe('Interface Mode Settings', () => {
  test('simple and advanced modes persist and change dashboard complexity', async ({ page }) => {
    await login(page);

    await page.goto('/settings');
    await page.getByTestId('interface-mode-select').locator('.p-selectbutton >> text=Simple Mode').click();
    await page.getByTestId('save-profile-button').click();
    await expect(page.getByText('Public identity updated.')).toBeVisible();

    await page.goto('/');
    await expect(page.locator('html')).toHaveClass(/interface-mode-simple/);
    await expect(page.getByTestId('dashboard-secondary-actions')).toHaveCount(0);
    await expect(page.getByTestId('interface-mode-badge')).toContainText('Simple Mode');

    await page.goto('/settings');
    await page.getByTestId('interface-mode-select').locator('.p-selectbutton >> text=Advanced Mode').click();
    await page.getByTestId('save-profile-button').click();
    await expect(page.getByText('Public identity updated.')).toBeVisible();

    await page.goto('/');
    await expect(page.locator('html')).toHaveClass(/interface-mode-advanced/);
    await expect(page.getByTestId('dashboard-secondary-actions')).toBeVisible();
    await expect(page.getByTestId('interface-mode-badge')).toContainText('Advanced Mode');

    await page.reload();
    await expect(page.locator('html')).toHaveClass(/interface-mode-advanced/);
    await expect(page.getByTestId('dashboard-secondary-actions')).toBeVisible();
  });
});
