import { expect, test } from '@playwright/test';

test.describe('Dashboard clarity hierarchy', () => {
  test('shows one dominant primary action and moves the rest into a secondary surface', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();

    await page.waitForURL('**/');
    await expect(page.getByTestId('dashboard-hero')).toBeVisible({ timeout: 30000 });

    await expect(page.getByTestId('dashboard-action-report')).toBeVisible();
    await expect(page.getByTestId('dashboard-primary-guidance')).toBeVisible();
    await expect(page.getByTestId('dashboard-secondary-actions')).toBeVisible();

    const hero = page.getByTestId('dashboard-hero');
    await expect(hero.getByTestId('dashboard-action-report')).toBeVisible();
    await expect(hero.getByTestId('dashboard-action-threads')).toHaveCount(0);
    await expect(hero.getByTestId('dashboard-action-blog')).toHaveCount(0);

    const secondarySurface = page.getByTestId('dashboard-secondary-actions');
    await expect(secondarySurface.getByTestId('dashboard-action-threads')).toBeVisible();
    await expect(secondarySurface.getByTestId('dashboard-action-blog')).toBeVisible();
    await expect(secondarySurface.getByTestId('dashboard-action-mine')).toBeVisible();
  });
});
