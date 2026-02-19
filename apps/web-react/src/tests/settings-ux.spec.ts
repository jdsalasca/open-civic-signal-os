import { test, expect } from '@playwright/test';

test.describe('Settings and UX Hardening Verification', () => {
  
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:3002/login');
    await page.waitForSelector('[data-testid="login-username-input"]');
    await page.fill('[data-testid="login-username-input"]', 'admin');
    await page.fill('[data-testid="login-password-input"]', 'admin12345');
    await page.click('[data-testid="login-submit-button"]');
    // Ensure we are fully logged in
    await expect(page.locator('[data-testid="dashboard-hero"]')).toBeVisible({ timeout: 10000 });
  });

  test('Language Switching (i18n) - English to Spanish', async ({ page }) => {
    await page.goto('http://localhost:3002/settings');
    await page.waitForSelector('text=System Settings');
    
    // Switch to Spanish
    await page.click('button:has-text("Español")');
    
    // Verify translation change
    await expect(page.getByText('Configuración del Sistema')).toBeVisible();
    
    // Navigate back to Dashboard via Nav Link
    await page.click('a[href="/"]');
    await expect(page.locator('[data-testid="dashboard-hero"] h1')).toContainText('Perspectivas');
  });

  test('Theme Toggling - Dark to Light persistence', async ({ page }) => {
    await page.goto('http://localhost:3002/settings');
    await page.waitForSelector('text=Interface Theme');
    
    const html = page.locator('html');
    
    // Switch to Light Mode
    await page.click('button:has-text("Light Mode")');
    await expect(html).toHaveClass(/light-theme/);
    
    // Reload to verify persistence
    await page.reload();
    await expect(html).toHaveClass(/light-theme/);
    
    // Revert to dark for other tests consistency
    await page.click('button:has-text("Dark Mode")');
    await expect(html).toHaveClass(/dark-theme/);
  });

  test('Role Switching - Active Identity update', async ({ page }) => {
    await page.goto('http://localhost:3002/settings');
    
    // Role switching is only visible for users with multiple roles (admin has 3)
    await page.waitForSelector('.p-dropdown');
    await page.click('.p-dropdown');
    await page.click('li:has-text("CITIZEN")');
    
    // Verify UI reflects CITIZEN
    await page.click('a[href="/"]');
    await expect(page.locator('[data-testid="citizen-support-card"]')).toBeVisible();
  });

  test('UX Hardening - Autocomplete and Ghost Rows', async ({ page }) => {
    // Check Autocomplete on Login (go there directly)
    await page.goto('http://localhost:3002/login');
    const usernameInput = page.locator('[data-testid="login-username-input"]');
    await expect(usernameInput).toHaveAttribute('autocomplete', 'username');
    
    // Back to Dashboard to check rows
    await page.fill('[data-testid="login-username-input"]', 'admin');
    await page.fill('[data-testid="login-password-input"]', 'admin12345');
    await page.click('[data-testid="login-submit-button"]');
    
    // Table should be visible and NO skeletons should be stuck
    const table = page.locator('[data-testid="signals-datatable"]');
    await expect(table).toBeVisible();
    await expect(page.locator('.p-skeleton')).toHaveCount(0, { timeout: 10000 });
  });
});
