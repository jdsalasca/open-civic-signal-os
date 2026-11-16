import { expect, test } from '@playwright/test';

// The exact case reported from Colombia: an English browser installed on a Spanish-speaking
// machine. The timezone is the signal that the person is in a Spanish-speaking place, so the app
// must open in Spanish; an explicit choice in the wheel must win and persist.
test.use({ locale: 'en-US', timezoneId: 'America/Bogota' });

test.describe('Language follows the place, not the installer', () => {
  test('opens in Spanish in a Spanish-speaking timezone and remembers an explicit switch', async ({ page }) => {
    await page.goto('/login');
    await expect(page.locator('html')).toHaveAttribute('lang', 'es');

    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();
    await page.waitForURL('**/');

    await expect(page.getByRole('heading', { level: 1 })).toContainText('Lo que pasa en');

    await page.getByTestId('theme-wheel-trigger').click();
    await page.getByTestId('language-wheel-en').click();
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('What is happening in');

    await page.reload();
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('What is happening in');
  });
});
