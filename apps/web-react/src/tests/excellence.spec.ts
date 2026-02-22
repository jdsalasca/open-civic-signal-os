import { test, expect } from '@playwright/test';

test.describe('Signal OS - High Quality E2E Suite', () => {

  test('Complete Quality Audit Flow with Verification', async ({ page }) => {
    const uniqueUser = `qa_expert_${Date.now()}_${Math.floor(Math.random() * 1_000_000)}`;
    
    // 1. Registration Flow
    await page.goto('/register');
    await page.getByTestId('register-username-input').fill(uniqueUser);
    await page.getByTestId('register-email-input').fill(`${uniqueUser}@yopmail.com`);
    await page.getByTestId('register-password-input').fill('SecurePass123!');
    await page.getByTestId('register-confirm-password-input').fill('SecurePass123!');
    await page.getByTestId('register-submit-button').click();

    // 2. Identity Verification (Activation Step)
    await expect(page).toHaveURL(/.*verify/);
    await page.locator('#verify-code').fill('123456');
    await page.getByRole('button', { name: /Verify/ }).click();

    // 3. Successful Login
    await page.goto('/login');
    await page.waitForSelector('[data-testid="login-card"]');
    await page.getByTestId('login-username-input').fill(uniqueUser);
    await page.getByTestId('login-password-input').fill('SecurePass123!');
    await page.getByTestId('login-submit-button').click();

    // Identity Welcome - Wait for hydration and dashboard load
    await page.waitForSelector('[data-testid="auth-loading"]', { state: 'detached' });
    await expect(page.getByTestId('welcome-message')).toContainText(uniqueUser, { timeout: 30000 });
    await expect(page.getByTestId('dashboard-freshness-badge')).toBeVisible();

    // 4. Settings: i18n & Theme Hardening
    await page.goto('/settings');
    await page.waitForLoadState('networkidle');
    await page.locator('.p-selectbutton >> text=Español').click();
    await expect(page.getByText('Configuración del Sistema')).toBeVisible();

    // Ensure the newly created user has an active community context before reporting
    await page.goto('/communities');
    await page.getByTestId('join-community-dropdown').click();
    await page.locator('.p-dropdown-item').first().click();
    await page.getByTestId('join-community-button').click();

    // 5. Contribution Flow (Spanish)
    const mobileMenuButton = page.locator('button[aria-label="Abrir menú de navegación"]');
    if (await mobileMenuButton.isVisible()) {
      await mobileMenuButton.click();
      await page.locator('.p-sidebar a[href="/report"]').click();
    } else {
      await page.click('a[href="/report"]');
    }
    
    await page.getByTestId('report-title-input').fill('Mejora de Alumbrado Público');
    await page.getByTestId('report-description-textarea').fill('Validación de flujo bilingüe y estabilidad de datos.');
    
    await page.getByTestId('report-category-dropdown').click();
    await page.locator('.p-dropdown-item >> text=Infraestructura').click();
    
    await page.getByTestId('report-submit-button').click();

    // 6. Dashboard Integrity
    await expect(page.locator('[data-testid="dashboard-hero"]')).toBeVisible();
    const table = page.locator('[data-testid="signals-datatable"]');
    await expect(table).toBeVisible();
    await expect(page.locator('.p-skeleton')).toHaveCount(0, { timeout: 10000 });

    // 7. Security Logout
    const desktopLogout = page.getByTestId('logout-button-desktop');
    const mobileLogout = page.getByTestId('logout-button-mobile');
    if (await desktopLogout.isVisible()) {
      await desktopLogout.click();
    } else {
      const mobileMenuButton = page
        .locator('button[aria-label="Abrir menú de navegación"], button[aria-label="Open navigation menu"]')
        .first();
      if (await mobileMenuButton.isVisible()) {
        await mobileMenuButton.click();
      }
      await mobileLogout.evaluate((el) => (el as HTMLElement).click());
    }
    await expect(page).toHaveURL(/.*login/);
  });
});
