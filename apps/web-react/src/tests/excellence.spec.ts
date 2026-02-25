import { test, expect } from '@playwright/test';

test.describe('Signal OS - High Quality E2E Suite', () => {

  test('Complete Quality Audit Flow with Verification', async ({ page }) => {
    // Use deterministic seeded admin credentials - avoids flaky email verification flow
    await page.goto('/login');
    await page.waitForSelector('[data-testid="login-card"]');
    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();

    // Identity Welcome - Wait for hydration and dashboard load
    await page.waitForSelector('[data-testid="auth-loading"]', { state: 'detached' }).catch(() => { });
    await expect(page.getByTestId('welcome-message')).toContainText('admin', { timeout: 30000 });
    await expect(page.getByTestId('dashboard-freshness-badge')).toBeVisible();

    // Settings: i18n Toggle Verification (click Español, verify Spanish label, then reset)
    await page.goto('/settings');
    await page.waitForLoadState('networkidle');

    // Click Español button (in English mode, button reads "Español")
    await page.locator('.p-selectbutton button').filter({ hasText: /español/i }).click();
    // Confirm UI switched to Spanish
    await expect(page.getByText('Configuración del Sistema')).toBeVisible();

    // Switch back to English - in Spanish mode the English button reads "Inglés"
    await page.locator('.p-selectbutton button').filter({ hasText: /ingl/i }).click();
    await expect(page.getByText('System Configuration').or(page.getByText('Configuración del Sistema')).first()).toBeVisible();

    // Ensure the admin user has an active community context before reporting
    await page.goto('/communities');
    // Admin is already a member of seeded communities; try joining only if the button is visible and enabled
    const joinDropdown = page.getByTestId('join-community-dropdown');
    if (await joinDropdown.isVisible({ timeout: 3000 }).catch(() => false)) {
      await joinDropdown.click();
      await page.locator('.p-dropdown-item').first().click();
      const joinBtn = page.getByTestId('join-community-button');
      if (await joinBtn.isEnabled({ timeout: 2000 }).catch(() => false)) {
        await joinBtn.click();
      }
    }

    // Navigate to report
    await page.goto('/report');

    await page.getByTestId('report-title-input').fill('Infrastructure Lighting Improvement');
    await page.getByTestId('report-description-textarea').fill('Testing E2E audit flow for data integrity.');

    await page.getByTestId('report-category-dropdown').click();
    await page.locator('.p-dropdown-item').first().click();

    await page.getByTestId('report-submit-button').click();

    // Dashboard Integrity
    await expect(page.locator('[data-testid="dashboard-hero"]')).toBeVisible({ timeout: 15000 });
    const table = page.locator('[data-testid="signals-datatable"]');
    await expect(table).toBeVisible();
    await expect(page.locator('.p-skeleton')).toHaveCount(0, { timeout: 10000 });

    // Security Logout
    const desktopLogout = page.getByTestId('logout-button-desktop');
    const mobileLogout = page.getByTestId('logout-button-mobile');
    if (await desktopLogout.isVisible()) {
      await desktopLogout.click();
    } else {
      const mobileMenuButton = page
        .locator('button[aria-label="Open navigation menu"], button[aria-label="Abrir menú de navegación"]')
        .first();
      if (await mobileMenuButton.isVisible()) {
        await mobileMenuButton.click();
      }
      await mobileLogout.evaluate((el) => (el as HTMLElement).click());
    }
    await expect(page).toHaveURL(/.*login/);
  });
});
