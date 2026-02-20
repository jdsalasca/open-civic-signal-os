import { test, expect } from '@playwright/test';

test.describe('Mobile Navigation Integrity (P0)', () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test('Drawer menu layering and opacity on mobile', async ({ page }) => {
    // Login as admin to see all nav links
    await page.goto('/login');
    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();

    await expect(page.getByTestId('welcome-message')).toBeVisible({ timeout: 15000 });

    // Open Drawer
    const menuButton = page.locator('button[aria-label="Open navigation menu"]');
    await expect(menuButton).toBeVisible();
    await menuButton.click();

    // Verify Sidebar visibility and opacity
    const sidebar = page.locator('.p-sidebar');
    await expect(sidebar).toBeVisible();
    
    // Check computed styles for opacity and background (P0-1)
    const background = await sidebar.evaluate((el) => window.getComputedStyle(el).backgroundColor);
    const opacity = await sidebar.evaluate((el) => window.getComputedStyle(el).opacity);
    
    // rgb(15, 23, 42) is #0f172a
    expect(background).toBe('rgb(15, 23, 42)');
    expect(opacity).toBe('1');

    // Verify Nav items are readable and present
    await expect(sidebar.getByText('Insights')).toBeVisible();
    await expect(sidebar.getByText('Moderation')).toBeVisible();

    // Verify backdrop mask presence
    const mask = page.locator('.p-sidebar-mask');
    await expect(mask).toBeVisible();
    const backdropFilter = await mask.evaluate((el) => {
      const style = window.getComputedStyle(el) as any;
      return style.backdropFilter || style.webkitBackdropFilter;
    });
    expect(backdropFilter).toContain('blur(8px)');

    // Close menu
    await page.keyboard.press('Escape');
    await expect(sidebar).not.toBeVisible();
  });
});
