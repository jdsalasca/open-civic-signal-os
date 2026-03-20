import { expect, test } from '@playwright/test';

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login');
  await page.getByTestId('login-username-input').fill('admin');
  await page.getByTestId('login-password-input').fill('admin12345');
  await page.getByTestId('login-submit-button').click();
  await page.waitForURL('**/');
  await expect(page.getByTestId('welcome-message')).toBeVisible({ timeout: 30000 });
}

test.describe('Navigation clarity', () => {
  test('desktop keeps only the core path visible until more tools is expanded', async ({ page }) => {
    test.skip(test.info().project.name === 'mobile-chrome', 'Desktop-only assertion');
    await login(page);

    const aside = page.locator('aside');
    await expect(aside.getByText('Start Here')).toBeVisible();
    await expect(aside.getByRole('link', { name: 'Home' })).toBeVisible();
    await expect(aside.getByRole('link', { name: 'Report' })).toBeVisible();
    await expect(aside.getByRole('link', { name: 'My Activity' })).toBeVisible();
    await expect(page.getByTestId('desktop-more-toggle')).toBeVisible();
    await expect(page.getByTestId('desktop-more-panel')).toHaveCount(0);

    await page.getByTestId('desktop-more-toggle').click();
    await expect(page.getByTestId('desktop-more-panel')).toBeVisible();
    await expect(aside.getByRole('link', { name: 'Shared Updates' })).toBeVisible();
    await expect(aside.getByRole('link', { name: 'Open Data' })).toBeVisible();
    await expect(aside.getByRole('link', { name: 'Official Updates' })).toBeVisible();
    await expect(aside.getByRole('link', { name: 'Community Talks' })).toBeVisible();
    await expect(aside.getByRole('link', { name: 'Preferences' })).toBeVisible();
  });

  test('mobile bottom nav stays short and routes extra options through the drawer', async ({ page, isMobile }) => {
    test.skip(!isMobile, 'Mobile-only assertion');
    await login(page);

    const bottomNav = page.locator('nav').filter({ has: page.getByTestId('mobile-more-toggle') });
    await expect(bottomNav.getByLabel('Home')).toBeVisible();
    await expect(bottomNav.getByLabel('Report')).toBeVisible();
    await expect(bottomNav.getByLabel('My Activity')).toBeVisible();
    await expect(page.getByTestId('mobile-more-toggle')).toBeVisible();
    await expect(bottomNav.getByLabel('Official Updates')).toHaveCount(0);

    await page.getByTestId('mobile-more-toggle').click();
    const drawer = page.locator('.p-sidebar');
    await expect(drawer).toBeVisible();
    await expect(page.getByTestId('mobile-drawer-guidance')).toBeVisible();
    await expect(drawer.getByRole('link', { name: 'Shared Updates' })).toBeVisible();
    await expect(drawer.getByRole('link', { name: 'Open Data' })).toBeVisible();
    await expect(drawer.getByRole('link', { name: 'Official Updates' })).toBeVisible();
    await expect(drawer.getByRole('link', { name: 'Community Talks' })).toBeVisible();
    await expect(drawer.getByRole('link', { name: 'Preferences' })).toBeVisible();
  });
});
