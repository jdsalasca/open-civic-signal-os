import { expect, test } from '@playwright/test';

test.describe('Report Form UX (P0)', () => {
  test('Report submit should guide users instead of failing silently', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();
    await page.waitForURL('**/');

    await page.goto('/report');
    await page.getByTestId('report-submit-button').click();
    await expect(page.getByText('This field is required.').first()).toBeVisible();

    await page.locator('#title').fill(`UX Report ${Date.now()}`);
    await page.locator('#description').fill('Report form should be explicit and reliable.');
    await page.locator('#category').click();
    await page.locator('.p-dropdown-item').first().click();

    await expect(page.getByTestId('report-submit-button')).toBeEnabled();
    await page.getByTestId('report-submit-button').click();
    await expect(page).toHaveURL(/.*\/$/);
  });
});
