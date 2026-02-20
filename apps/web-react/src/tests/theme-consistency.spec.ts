import { test, expect } from '@playwright/test';

test.describe('Theme Consistency (P1)', () => {
  test('Light/Dark switch keeps consistent visual tokens', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();

    await page.waitForURL('**/');
    await expect(page.getByTestId('welcome-message')).toBeVisible();

    await page.goto('/settings');
    await page.waitForLoadState('networkidle');

    await page.locator('.p-selectbutton >> text=Light Mode').click();
    await expect.poll(async () =>
      page.evaluate(() => document.documentElement.classList.contains('light-theme'))
    ).toBeTruthy();

    await page.goto('/');
    await page.waitForLoadState('networkidle');
    const lightCardBg = await page.locator('[data-testid="dashboard-hero"]').evaluate((el) => {
      const style = getComputedStyle(el);
      return style.color;
    });
    expect(lightCardBg).not.toBe('');

    await page.goto('/settings');
    await page.waitForLoadState('networkidle');
    await page.locator('.p-selectbutton >> text=Dark Mode').click();
    await expect.poll(async () =>
      page.evaluate(() => document.documentElement.classList.contains('dark-theme'))
    ).toBeTruthy();

    await page.goto('/');
    await expect(page.getByTestId('signals-datatable')).toBeVisible();
    await expect(page.getByTestId('dashboard-freshness-badge')).toBeVisible();
  });
});
