import { expect, test } from '@playwright/test';

test.describe('Accessibility keyboard baseline', () => {
  test('skip link moves focus to main content and route remains keyboard navigable', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();

    await page.waitForURL('**/');
    await expect(page.getByTestId('welcome-message')).toBeVisible({ timeout: 30000 });

    await page.keyboard.press('Tab');
    await expect(page.getByTestId('skip-to-content-link')).toBeFocused();

    await page.keyboard.press('Enter');
    await expect
      .poll(async () =>
        page.evaluate(() => {
          const main = document.querySelector('[data-testid="main-content"]');
          return Boolean(main?.contains(document.activeElement));
        })
      )
      .toBe(true);

    await page.goto('/report');
    await expect(page.getByTestId('report-title-input')).toBeVisible();

    await page.keyboard.press('Tab');
    await expect(page.getByTestId('skip-to-content-link')).toBeFocused();

    await page.keyboard.press('Enter');
    await expect
      .poll(async () =>
        page.evaluate(() => {
          const main = document.querySelector('[data-testid="main-content"]');
          return Boolean(main?.contains(document.activeElement));
        })
      )
      .toBe(true);
  });
});
